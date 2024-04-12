package se.liu.albhe576.project;

public class ReturnStmt extends Stmt{

    @Override
    public String toString() {
        return String.format("return %s;", expr);
    }

    private final Expr expr;
    public ReturnStmt(Expr expr){
        this.expr = expr;

    }
}
