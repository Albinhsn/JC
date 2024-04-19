package se.liu.albhe576.project;

public class AssignExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s = %s",variable, value);
    }
    private final Expr variable;
    private final Expr value;

    public AssignExpr(Expr variable, Expr value, int line, String file){
        super(line, file);
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
        quads.createPush(pushed);
        quads.addAll(variableQuads);
        Symbol valResult = quads.getLastResult();

        if(!memberSymbol.type.isFloatingPoint()){
            quads.addQuad(QuadOp.MOV_REG_CA, valResult, null, Compiler.generateSymbol(valResult.type));
        }

        Symbol popped = Compiler.generateSymbol(result.type);
        quads.createPop(popped);
        quads.addQuad(QuadOp.SET_FIELD, memberSymbol, struct, result);

    }
    private void compileStoreDereference(QuadList valueQuads, QuadList variableQuads) {
        Symbol dereferenced = variableQuads.getLastOperand1();
        Symbol valueSymbol = valueQuads.getLastResult();
        variableQuads.removeLastQuad();
        valueQuads.createSetupBinary(variableQuads, valueSymbol, dereferenced);

        valueQuads.addQuad(QuadOp.STORE_INDEX, valueSymbol, dereferenced, null);
    }

    private void compileStoreIndex(QuadList quads, QuadList variableQuads) throws CompileException{
        Symbol res = variableQuads.getLastOperand2();
        Symbol toStore = quads.getLastResult();

        quads.createPush(toStore);

        DataType resType = res.type.getTypeFromPointer();
        quads.addAll(variableQuads);
        quads.removeLastQuad();
        quads.addQuad(QuadOp.MOV_REG_CA, quads.getLastResult(), null, Compiler.generateSymbol(DataType.getInt()));
        // ToDo check if they can be converted
        if(!resType.isSameType(toStore.type)){
            quads.createPop(toStore);
            if(resType.isFloatingPoint()){
                Symbol newToStore = Compiler.generateSymbol(DataType.getFloat());
                quads.addQuad(QuadOp.CVTSI2SD, toStore, null, newToStore);
                toStore = newToStore;
            }else if(toStore.type.isFloatingPoint()){
                Symbol newToStore = Compiler.generateSymbol(DataType.getInt());
                quads.addQuad(QuadOp.CVTTSD2SI, toStore, null, newToStore);
                toStore = newToStore;
            }else{
                throw new CompileException(String.format("What are you trying to do?, line %d", this.line));
            }
        }else{
            quads.createPop(toStore);
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


        if(lastOp == QuadOp.GET_FIELD){
            this.compileStoreField(symbolTable, quads, variableQuads);
        }else if(lastOp == QuadOp.DEREFERENCE){
            this.compileStoreDereference(quads, variableQuads);
        }
        else if (valueType.type.isStruct()){
            quads.createSetupBinary(variableQuads, valueType, variableType);
            quads.addQuad(QuadOp.MOVE_STRUCT, valueType, variableType, null);
        } else if(variableQuads.size() == 1){
            Symbol res = quads.getLastResult();
            quads.addQuad(QuadOp.STORE, res, null, variableQuads.getLastResult());
        } else if(lastOp == QuadOp.INDEX){
            this.compileStoreIndex(quads, variableQuads);
        }else{
            this.error("How could this happen to me");
        }

    }
}
