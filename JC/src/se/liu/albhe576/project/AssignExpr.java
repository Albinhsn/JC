package se.liu.albhe576.project;

public class AssignExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s = %s",variable, value);
    }
    private final Expr variable;
    private final Expr value;

    public AssignExpr(Expr variable, Expr value, int line){
        super(line);
        this.variable = variable;
        this.value = value;
    }

    private QuadList compileStoreField(SymbolTable symbolTable, QuadList valueQuads, QuadList variableQuads) throws UnknownSymbolException {
        Quad lastQuad =  variableQuads.getLastQuad();
        variableQuads.removeLastQuad();
        Symbol struct = lastQuad.operand1;
        Symbol op2 = lastQuad.operand2;
        Symbol result = lastQuad.result;
        Symbol memberSymbol = symbolTable.getMemberSymbol(struct, op2.name);


        Symbol pushed = Compiler.generateSymbol(struct.type);
        variableQuads.addQuad(QuadOp.PUSH, result, null, pushed);
        variableQuads.concat(valueQuads);
        Symbol valResult = valueQuads.getLastResult();

        if(memberSymbol.type.type != DataTypes.FLOAT){
            variableQuads.addQuad(QuadOp.MOV_REG_CA, valResult, null, Compiler.generateSymbol(valResult.type));
        }

        Symbol popped = Compiler.generateSymbol(result.type);
        variableQuads.addQuad(QuadOp.POP, pushed, null, popped);
        variableQuads.addQuad(QuadOp.SET_FIELD, struct, memberSymbol, result);

        return variableQuads;
    }
    private QuadList compileStoreDereference(QuadList valueQuads, QuadList variableQuads) throws CompileException {

        Symbol dereferenced = variableQuads.getLastOperand1();
        Symbol valueSymbol = valueQuads.getLastResult();
        variableQuads.removeLastQuad();

        // rax is value
        valueQuads.addQuad(QuadOp.PUSH, valueSymbol, null, valueSymbol);

        // rcx is pointer
        valueQuads.concat(variableQuads);
        valueQuads.addQuad(QuadOp.MOV_REG_CA, dereferenced, null, dereferenced);
        valueQuads.addQuad(QuadOp.POP, valueSymbol, null, valueSymbol);
        valueQuads.addQuad(QuadOp.STORE_INDEX, valueSymbol, dereferenced, null);

        return valueQuads;
    }

    private QuadList compileStoreIndex(QuadList variableQuads, QuadList valueQuads) throws CompileException {
        Symbol res = variableQuads.getLastOperand2();
        Symbol toStore = valueQuads.getLastResult();


        valueQuads.addQuad(QuadOp.PUSH, toStore, null, toStore);


        DataType resType = res.type.getTypeFromPointer();
        variableQuads.removeLastQuad();
        variableQuads.addQuad(QuadOp.MOV_REG_CA, variableQuads.getLastResult(), null, Compiler.generateSymbol(DataType.getInt()));
        // ToDo check if they can be converted
        if(!resType.isSameType(toStore.type)){
            variableQuads.addQuad(QuadOp.POP, toStore, null, toStore);
            if(resType.type == DataTypes.FLOAT){
                Symbol newToStore = Compiler.generateSymbol(DataType.getFloat());
                variableQuads.addQuad(QuadOp.CVTSI2SD, toStore, null, newToStore);
                toStore = newToStore;
            }else if(toStore.type.type == DataTypes.FLOAT){
                Symbol newToStore = Compiler.generateSymbol(DataType.getInt());
                variableQuads.addQuad(QuadOp.CVTTSD2SI, toStore, null, newToStore);
                toStore = newToStore;
            }else{
                throw new CompileException("What are you trying to do?");
            }
        }else{
            variableQuads.addQuad(QuadOp.POP, toStore, null, toStore);
        }

        valueQuads.concat(variableQuads);
        valueQuads.addQuad(QuadOp.STORE_INDEX, Compiler.generateSymbol(res.type.getTypeFromPointer()), toStore, res);

        return valueQuads;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {
        QuadList valueQuads = value.compile(symbolTable);
        QuadList variableQuads = variable.compile(symbolTable);

        QuadOp lastOp = variableQuads.getLastOp();
        Symbol valueType = valueQuads.getLastOperand1();
        Symbol variableType = variableQuads.getLastOperand1();


        // There is a difference when assinging a value whose location is on the stack
        // versus something that's accessed through a pointer to something
        // This is just looking essentially if the last thing in the variable is a get field
        if(lastOp == QuadOp.GET_FIELD){
            return this.compileStoreField(symbolTable, valueQuads, variableQuads);

        }else if(lastOp == QuadOp.DEREFERENCE){
            return this.compileStoreDereference(valueQuads, variableQuads);
        }
        else if (valueType.type.type == DataTypes.STRUCT){
            valueQuads.addQuad(QuadOp.PUSH, valueType, null, valueType);
            valueQuads.concat(variableQuads);
            valueQuads.addQuad(QuadOp.MOV_REG_CA, variableType, null, variableType);
            valueQuads.addQuad(QuadOp.POP, valueType, null, valueType);
            valueQuads.addQuad(QuadOp.MOVE_STRUCT, valueType, variableType, null);

            return valueQuads;

        } else if(variableQuads.size() == 1){
            // If we're just storing a variable on the stack we don't care to load the variable at all
            // So just store it directly instead, ToDo type check though
            // ... unless it's a struct :)
            Symbol res = valueQuads.getLastResult();
            valueQuads.addQuad(QuadOp.STORE, res, null, variableQuads.getLastResult());
            return valueQuads;
        } else if(lastOp == QuadOp.INDEX){
            return this.compileStoreIndex(variableQuads, valueQuads);
        }

        // Figure out if legal?
        Symbol res = valueQuads.getLastResult();

        valueQuads.concat(variableQuads);
        valueQuads.addQuad(QuadOp.STORE, res, null, variableQuads.getLastResult());

        return valueQuads;
    }
}
