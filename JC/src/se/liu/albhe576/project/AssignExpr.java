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

    private void compileStoreField(SymbolTable symbolTable, QuadList quads, QuadList variableQuads) throws UnknownSymbolException {
        Quad lastQuad =  variableQuads.getLastQuad();
        variableQuads.removeLastQuad();
        Symbol struct = lastQuad.operand1;
        Symbol op2 = lastQuad.operand2;
        Symbol result = lastQuad.result;
        Symbol memberSymbol = symbolTable.getMemberSymbol(struct, op2.name);


        Symbol pushed = Compiler.generateSymbol(struct.type);
        quads.addQuad(QuadOp.PUSH, result, null, pushed);
        quads.addAll(variableQuads);
        Symbol valResult = quads.getLastResult();

        if(memberSymbol.type.type != DataTypes.FLOAT){
            quads.addQuad(QuadOp.MOV_REG_CA, valResult, null, Compiler.generateSymbol(valResult.type));
        }

        Symbol popped = Compiler.generateSymbol(result.type);
        quads.addQuad(QuadOp.POP, pushed, null, popped);
        quads.addQuad(QuadOp.SET_FIELD, memberSymbol, struct, result);

    }
    private void compileStoreDereference(QuadList valueQuads, QuadList variableQuads) throws CompileException {

        Symbol dereferenced = variableQuads.getLastOperand1();
        Symbol valueSymbol = valueQuads.getLastResult();
        variableQuads.removeLastQuad();

        // rax is value
        valueQuads.addQuad(QuadOp.PUSH, valueSymbol, null, valueSymbol);

        // rcx is pointer
        valueQuads.addAll(variableQuads);
        valueQuads.addQuad(QuadOp.MOV_REG_CA, dereferenced, null, dereferenced);
        valueQuads.addQuad(QuadOp.POP, valueSymbol, null, valueSymbol);
        valueQuads.addQuad(QuadOp.STORE_INDEX, valueSymbol, dereferenced, null);

    }

    private void compileStoreIndex(QuadList quads, QuadList variableQuads) throws CompileException, UnexpectedTokenException, UnknownSymbolException, InvalidOperation {
        Symbol res = variableQuads.getLastOperand2();
        Symbol toStore = quads.getLastResult();


        quads.addQuad(QuadOp.PUSH, toStore, null, toStore);


        DataType resType = res.type.getTypeFromPointer();
        quads.addAll(variableQuads);
        quads.removeLastQuad();
        quads.addQuad(QuadOp.MOV_REG_CA, quads.getLastResult(), null, Compiler.generateSymbol(DataType.getInt()));
        // ToDo check if they can be converted
        if(!resType.isSameType(toStore.type)){
            quads.addQuad(QuadOp.POP, toStore, null, toStore);
            if(resType.type == DataTypes.FLOAT){
                Symbol newToStore = Compiler.generateSymbol(DataType.getFloat());
                quads.addQuad(QuadOp.CVTSI2SD, toStore, null, newToStore);
                toStore = newToStore;
            }else if(toStore.type.type == DataTypes.FLOAT){
                Symbol newToStore = Compiler.generateSymbol(DataType.getInt());
                quads.addQuad(QuadOp.CVTTSD2SI, toStore, null, newToStore);
                toStore = newToStore;
            }else{
                throw new CompileException("What are you trying to do?");
            }
        }else{
            quads.addQuad(QuadOp.POP, toStore, null, toStore);
        }

        quads.addQuad(QuadOp.STORE_INDEX, Compiler.generateSymbol(res.type.getTypeFromPointer()), toStore, res);
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {
        value.compile(symbolTable, quads);
        QuadList variableQuads = new QuadList();
        variable.compile(symbolTable, variableQuads);

        QuadOp lastOp = variableQuads.getLastOp();
        Symbol valueType = quads.getLastOperand1();
        Symbol variableType = variableQuads.getLastOperand1();


        // There is a difference when assinging a value whose location is on the stack
        // versus something that's accessed through a pointer to something
        // This is just looking essentially if the last thing in the variable is a get field
        if(lastOp == QuadOp.GET_FIELD){
            this.compileStoreField(symbolTable, quads, variableQuads);

        }else if(lastOp == QuadOp.DEREFERENCE){
            this.compileStoreDereference(quads, variableQuads);
        }
        else if (valueType.type.type == DataTypes.STRUCT){
            quads.addQuad(QuadOp.PUSH, valueType, null, valueType);
            quads.addAll(variableQuads);
            quads.addQuad(QuadOp.MOV_REG_CA, variableType, null, variableType);
            quads.addQuad(QuadOp.POP, valueType, null, valueType);
            quads.addQuad(QuadOp.MOVE_STRUCT, valueType, variableType, null);

        } else if(variableQuads.size() == 1){
            // If we're just storing a variable on the stack we don't care to load the variable at all
            // So just store it directly instead, ToDo type check though
            // ... unless it's a struct :)
            Symbol res = quads.getLastResult();
            quads.addQuad(QuadOp.STORE, res, null, variableQuads.getLastResult());
        } else if(lastOp == QuadOp.INDEX){
            this.compileStoreIndex(quads, variableQuads);
        }else{
            // Figure out if legal?
            Symbol res = quads.getLastResult();

            variable.compile(symbolTable, quads);
            quads.addQuad(QuadOp.STORE, res, null, variableQuads.getLastResult());
        }

    }
}
