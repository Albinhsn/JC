package se.liu.albhe576.project;

public class GroupedExpr extends Expr{
    @Override
    public String toString() {
        return String.format("(%s)", expr);
    }

    private final Expr expr;
    public GroupedExpr(Expr expr, int line, String file){
        super(line, file);
        this.expr = expr;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        expr.compile(symbolTable, quads);
    }
}
