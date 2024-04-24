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
        Symbol value        = quads.getLastResult();

        value = convertValue(value, memberSymbol, quads);
        quads.createSetupBinary(variableQuads, value, variableQuads.getLastResult());
        quads.createSetField(memberSymbol, lastQuad.operand1());
    }

    public static void compileStoreDereferenced(QuadList valueQuads, QuadList variableQuads) {
        Quad lastVariableQuad = variableQuads.pop();
        Symbol dereferenced = lastVariableQuad.operand1();

        Symbol valueResult = convertValue(valueQuads.getLastResult(), lastVariableQuad.result(), valueQuads);

        valueQuads.createSetupBinary(variableQuads, valueResult, dereferenced);
        valueQuads.createStoreIndex(valueResult, dereferenced);
    }

    public static void compileStoreIndex(QuadList quads, QuadList variableQuads) throws CompileException{
        Quad lastVariableQuad   = variableQuads.pop();
        Symbol res              = lastVariableQuad.operand2();
        Symbol toStore          = quads.getLastResult();

        Symbol result = Compiler.generateSymbol(res.type.getTypeFromPointer());

        quads.createSetupBinary(variableQuads, toStore, variableQuads.getLastResult());
        toStore = convertValue(toStore, result, quads);
        quads.createStoreIndex(result, toStore);
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        value.compile(symbolTable, quads);

        QuadList variableQuads = new QuadList();
        variable.compile(symbolTable, variableQuads);

        Symbol valueResult = quads.getLastResult();
        Symbol variableType = variableQuads.getLastResult();

        boolean same        = variableType.type.isSameType(valueResult.type);
        boolean canBeCasted = variableType.type.canBeCastedTo(valueResult.type);
        boolean isNull      = quads.getLastOperand1().isNull();
        if(!(same || canBeCasted || isNull)){
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
                    convertValue(valueResult, variableType, quads);
                    quads.createStore(variableQuads.getLastOperand1());
                }
            }
        }
    }
}
