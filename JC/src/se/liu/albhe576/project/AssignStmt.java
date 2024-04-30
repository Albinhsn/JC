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

    @Override
    public void compile(SymbolTable symbolTable, QuadList valueQuads) throws CompileException {
        value.compile(symbolTable, valueQuads);
        Symbol valueResult = valueQuads.getLastResult();

        QuadList variableQuads = new QuadList();
        variable.compile(symbolTable, variableQuads);
        Symbol variable = variableQuads.getLastOperand1();
        Symbol variableType = variableQuads.getLastResult();

        if(isInvalidAssignment(variableType, valueResult, valueQuads.getLastOperand1())){
            Compiler.error(String.format("Trying to assign type %s to %s", valueResult.type, variableType.type), line, file);
        }
        if(!valueResult.type.isSameType(variableType.type)){
            valueQuads.createConvert(valueResult, variableType.type);
        }

        if(variableQuads.getLastOp() == QuadOp.LOAD_I){
            Quad loaded = variableQuads.pop();
            variableQuads.createLoadPointer(loaded.operand1());
        }

        valueQuads.addAll(variableQuads);
        valueQuads.createAssign(valueResult, variable);

    }
}
