package se.liu.albhe576.project;

import java.util.List;

public class GroupedExpr implements Expr{
    @Override
    public String toString() {
        return String.format("(%s)", expr);
    }

    private final Expr expr;
    public GroupedExpr(Expr expr){
        this.expr = expr;
    }

}
