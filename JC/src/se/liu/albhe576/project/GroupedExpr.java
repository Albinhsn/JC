package se.liu.albhe576.project;

public class GroupedExpr extends Expr{
    private final Expr expr;
    public GroupedExpr(Expr expr){
        this.expr = expr;
    }
}
