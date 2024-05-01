package se.liu.albhe576.project;

public class LogicalExpr extends Expr{

    private final Expr left;
    private final Expr right;
    private final Token op;
    public LogicalExpr(Expr left, Expr right, Token op, int line, String file){
        super(line, file);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException {
        left.compile(symbolTable, quads);
        Symbol leftResult = quads.getLastResult();
        right.compile(symbolTable, quads);
        Symbol rightResult = quads.getLastResult();

        quads.createLogical(symbolTable, leftResult, rightResult, this.op.type());

    }
}
