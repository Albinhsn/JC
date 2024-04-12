package se.liu.albhe576.project;

public class GroupedExpr extends Expr{
    @Override
    public String toString() {
        return String.format("(%s)", expr);
    }

    private Expr expr;
    public GroupedExpr(Expr expr){
        this.expr = expr;
    }
}
