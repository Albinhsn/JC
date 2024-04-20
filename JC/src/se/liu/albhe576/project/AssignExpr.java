package se.liu.albhe576.project;

public class AssignExpr extends Expr{
    private final Expr variable;
    private final Expr value;

    public AssignExpr(Expr variable, Expr value, int line, String file){
        super(line, file);
        this.variable = variable;
        this.value = value;
    }

    private void compileStoreField(SymbolTable symbolTable, QuadList quads, QuadList variableQuads) throws CompileException {
        Quad lastQuad =  variableQuads.pop();

        Symbol struct = lastQuad.operand1;
        Symbol memberSymbol = symbolTable.getMemberSymbol(lastQuad.operand1, lastQuad.operand2.name);
        Symbol value = quads.getLastResult();

        if(value.type.isFloatingPoint() && !memberSymbol.type.isFloatingPoint()){
             value = quads.createConvertFloatToInt(value);
        }
        else if(memberSymbol.type.isFloatingPoint() && !value.type.isFloatingPoint()){
            value = quads.createConvertIntToFloat(value);
        }

        quads.createPush(value);
        quads.addAll(variableQuads);
        Symbol varResult = quads.getLastResult();

        if(!memberSymbol.type.isFloatingPoint()){
            quads.createMovRegisterAToC(varResult);
        }

        quads.createPop(value);
        quads.createSetField(memberSymbol, struct);
    }

    private void compileStoreDereference(QuadList valueQuads, QuadList variableQuads) {
        Symbol dereferenced = variableQuads.getLastOperand1();
        Symbol valueSymbol = valueQuads.getLastResult();
        variableQuads.removeLastQuad();
        valueQuads.createSetupBinary(variableQuads, valueSymbol, dereferenced);

        valueQuads.createStoreIndex(valueSymbol, dereferenced);
    }

    private void compileStoreIndex(QuadList quads, QuadList variableQuads) throws CompileException{
        Symbol res = variableQuads.getLastOperand2();
        Symbol toStore = quads.getLastResult();

        quads.createPush(toStore);

        DataType resType = res.type.getTypeFromPointer();
        quads.addAll(variableQuads);
        quads.removeLastQuad();
        quads.createMovRegisterAToC(quads.getLastResult());
        // ToDo check if they can be converted
        if(!resType.isSameType(toStore.type)){
            quads.createPop(toStore);
            if(resType.isFloatingPoint()){
                toStore = quads.createConvertIntToFloat(toStore);
            }else if(toStore.type.isFloatingPoint()){
                toStore = quads.createConvertFloatToInt(toStore);
            }else{
                this.error("What are you trying to do?");
            }
        }else{
            quads.createPop(toStore);
        }

        quads.createStoreIndex(Compiler.generateSymbol(res.type.getTypeFromPointer()), toStore);
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        // Check types
        // foo = 5
        // *foo = 5
        // foo[5] = 5
        // foo.a = 5
        // foo.bar = bar2

        value.compile(symbolTable, quads);
        QuadList variableQuads = new QuadList();
        variable.compile(symbolTable, variableQuads);

        QuadOp lastOp = variableQuads.getLastOp();
        Symbol valueType = quads.getLastOperand1();
        Symbol valueResult = quads.getLastResult();
        Symbol variableType = variableQuads.getLastOperand1();



        if(lastOp == QuadOp.GET_FIELD){
            this.compileStoreField(symbolTable, quads, variableQuads);
        }else if(lastOp == QuadOp.DEREFERENCE){
            this.compileStoreDereference(quads, variableQuads);
        }
        else if (valueType.type.isStruct()){
            quads.createSetupBinary(variableQuads, valueResult, variableType);
            quads.addQuad(QuadOp.MOVE_STRUCT, valueResult, variableType, null);
        } else if(variableQuads.size() == 1){
            quads.createStore(variableQuads.getLastOperand1());
        } else if(lastOp == QuadOp.INDEX){
            this.compileStoreIndex(quads, variableQuads);
        }else{
            this.error("How could this happen to me");
        }

    }
}
