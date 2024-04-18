package se.liu.albhe576.project;

import java.util.List;

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
        // Check whether or not the thing we're assigning to is a struct dotExpr already?
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

    @Override
    public QuadList compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {
        QuadList valueQuads = value.compile(symbolTable);
        QuadList variableQuads = variable.compile(symbolTable);


        // There is a difference when assinging a value whose location is on the stack
        // versus something that's accessed through a pointer to something
        // This is just looking essentially if the last thing in the variable is a get field
        if(variableQuads.getLastOp() == QuadOp.GET_FIELD){
            return this.compileStoreField(symbolTable, valueQuads, variableQuads);

        }else if(variableQuads.size() == 1){
            // If we're just storing a variable on the stack we don't care to load the variable at all
            // So just store it directly instead, ToDo type check though
            Symbol res = valueQuads.getLastResult();
            valueQuads.addQuad(QuadOp.STORE, res, null, variableQuads.getLastResult());
            return valueQuads;
        }if(variableQuads.getLastOp() == QuadOp.INDEX){
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

        // Figure out if legal?
        Symbol res = valueQuads.getLastResult();

        valueQuads.concat(variableQuads);
        valueQuads.addQuad(QuadOp.STORE, res, null, variableQuads.getLastResult());

        return valueQuads;
    }
}
