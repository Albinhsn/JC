package se.liu.albhe576.project;

public class ReturnStmt extends Stmt{

    private final Expr expr;
    public ReturnStmt(Expr expr){
        this.expr = expr;

    }
}
