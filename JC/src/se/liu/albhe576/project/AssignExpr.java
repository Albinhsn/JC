package se.liu.albhe576.project;

import java.util.List;

public class AssignExpr implements  Expr{
    @Override
    public String toString() {
        return String.format("%s = %s",variableExpr, value);
    }
    private final Expr variableExpr;
    private final Expr value;

    public AssignExpr(Expr variableExpr, Expr value){
        this.variableExpr = variableExpr;
        this.value = value;
    }
}
