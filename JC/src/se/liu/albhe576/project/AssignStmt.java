package se.liu.albhe576.project;

public class AssignStmt extends Stmt{
    private final Expr variable;
    private final Expr value;

    public AssignStmt(Expr variable, Expr value, int line, String file){
        super(line, file);
        this.variable = variable;
        this.value = value;
    }

    public static Symbol convertValue(Symbol value, Symbol target, QuadList quads) {
        if(value.type.isByte() && !target.type.isByte()){
            value = quads.createConvertByteToInt(value);
        }

        if (value.type.isFloatingPoint() && !target.type.isFloatingPoint()) {
            value = quads.createConvertFloatToInt(value);
        } else if (target.type.isFloatingPoint() && !value.type.isFloatingPoint()) {
            value = quads.createConvertIntToFloat(value);
        }

        if(target.type.isByte() && value.type.isInteger()){
            value = Compiler.generateSymbol(DataType.getByte());
        }

        return value;
    }
    public static void compileStoreField(QuadList quads, QuadList variableQuads) {
        Quad   lastQuad     = variableQuads.pop();
        Symbol memberSymbol = lastQuad.operand2();

        quads.createSetupBinary(variableQuads, quads.getLastResult(), variableQuads.getLastResult(),  variableQuads.getLastResult());
        quads.createSetField(memberSymbol, lastQuad.operand1());
    }

    public static void compileStoreDereferenced(QuadList valueQuads, QuadList variableQuads) {
        variableQuads.removeLastQuad();
        Symbol target = Compiler.generateSymbol(variableQuads.getLastResult().type.getTypeFromPointer());
        storeIndex(valueQuads, variableQuads, valueQuads.getLastResult(), variableQuads.getLastResult(), target);
    }


    public static void compileStoreIndex(QuadList quads, QuadList variableQuads) {
        Quad lastVariableQuad   = variableQuads.pop();
        Symbol res              = lastVariableQuad.operand2();

        storeIndex(quads, variableQuads, quads.getLastResult(), res,Compiler.generateSymbol(res.type.getTypeFromPointer()));
    }
    private static void storeIndex(QuadList quads, QuadList variableQuads, Symbol toStore, Symbol result, Symbol target){
        toStore = quads.createSetupBinary(variableQuads, toStore, variableQuads.getLastResult(), target);
        quads.createStoreIndex(toStore, result);
    }

    public static void createStore(SymbolTable symbolTable, Expr variableExpr, QuadList valueQuads, Quad lastQuad) throws CompileException {
        QuadList variableQuads = new QuadList();
        variableExpr.compile(symbolTable, variableQuads);

        switch(lastQuad.op()){
            case DEREFERENCE -> AssignStmt.compileStoreDereferenced(valueQuads, variableQuads);
            case GET_FIELD-> AssignStmt.compileStoreField(valueQuads, variableQuads);
            case INDEX -> AssignStmt.compileStoreIndex(valueQuads, variableQuads);
            default -> valueQuads.createStoreVariable(lastQuad.operand1());
        }
    }
    public static boolean isInvalidAssignment(Symbol variableType, Symbol valueResult, Symbol lastOperand){
        return !(variableType.type.canBeCastedTo(valueResult.type)  || lastOperand.isNull());
    }


    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        value.compile(symbolTable, quads);

        QuadList variableQuads = new QuadList();
        variable.compile(symbolTable, variableQuads);

        Symbol valueResult = quads.getLastResult();
        Symbol variableType = variableQuads.getLastResult();

        if(isInvalidAssignment(variableType, valueResult, quads.getLastOperand1())){
            Compiler.error(String.format("Trying to assign type %s to %s", valueResult.type.name, variableType.type.name), line, file);
        }


        switch (variableQuads.getLastOp()){
            case GET_FIELD   -> compileStoreField(quads, variableQuads);
            case DEREFERENCE -> compileStoreDereferenced(quads, variableQuads);
            case INDEX       -> compileStoreIndex(quads, variableQuads);
            default          -> {
                if(valueResult.type.isStruct()){
                    quads.createSetupBinary(variableQuads, valueResult, variableType);
                    quads.addQuad(QuadOp.MOVE_STRUCT, valueResult, variableType, null);
                }else{
                    quads.createSetupBinary(variableQuads, valueResult, variableType);
                    quads.createStoreVariable(variableQuads.getLastOperand1());
                }
            }
        }
    }
}
