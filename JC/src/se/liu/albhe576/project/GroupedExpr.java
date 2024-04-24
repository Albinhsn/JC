package se.liu.albhe576.project;

public class GroupedExpr extends Expr{
    private final Expr expr;
    public GroupedExpr(Expr expr, int line, String file){
        super(line, file);
        this.expr = expr;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        expr.compile(symbolTable, quads);
    }
}
