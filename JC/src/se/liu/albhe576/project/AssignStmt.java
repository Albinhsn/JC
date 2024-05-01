package se.liu.albhe576.project;

public class AssignStmt extends Stmt{
    private final Expr variable;
    private final Expr value;

    public AssignStmt(Expr variable, Expr value, int line, String file){
        super(line, file);
        this.variable = variable;
        this.value = value;
    }

    public static boolean isInvalidAssignment(Symbol variableType, Symbol valueResult, Symbol lastOperand){
        return !(variableType.type.canBeConvertedTo(valueResult.type)  || lastOperand.isNull());
    }

    public static Symbol setupAssignment(SymbolTable symbolTable, QuadList variableQuads, Symbol variableSymbol) throws CompileException {
        Quad loaded = variableQuads.pop();
        QuadOp lastOp = loaded.op();
        if(lastOp == QuadOp.LOAD){
            variableQuads.createLoadPointer(symbolTable, loaded.operand1());
        }else if(lastOp == QuadOp.LOAD_MEMBER){
            variableQuads.createLoadMemberPointer(symbolTable, loaded.operand1(), loaded.operand2(), loaded.result().type);
            variableSymbol = loaded.result();
        }else if(lastOp == QuadOp.INDEX){
            variableQuads.createReferenceIndex(symbolTable, loaded.operand1(), loaded.operand2());
            variableSymbol = symbolTable.generateSymbol(loaded.operand1().type.getTypeFromPointer());
        }
        return variableSymbol;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList valueQuads) throws CompileException {
        value.compile(symbolTable, valueQuads);
        Symbol valueResult      = valueQuads.getLastResult();
        Symbol valueOperand = valueQuads.getLastOperand1();

        QuadList variableQuads  = new QuadList();
        variable.compile(symbolTable, variableQuads);
        Symbol variable         = variableQuads.getLastOperand1();
        Symbol variableType     = variableQuads.getLastResult();

        if(isInvalidAssignment(variableType, valueResult, valueOperand)){
            Compiler.error(String.format("Trying to assign type %s to %s", valueResult.type, variableType.type), line, file);
        }

        if(!valueResult.type.isSameType(variableType.type)){
            valueResult = valueQuads.createConvert(symbolTable, valueResult, variableType.type);
        }

        variable = setupAssignment(symbolTable, variableQuads, variable);
        valueQuads.addAll(variableQuads);
        valueQuads.createAssign(valueResult, variable);
    }
}
