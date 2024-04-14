package se.liu.albhe576.project;

import java.util.List;

public class AugmentedExpr implements  Expr{

    @Override
    public String toString() {
        return String.format("%s %s %s", target, op.literal, value);
    }


    private final Token op;
    private final Expr target;
    private final Expr value;
    public AugmentedExpr(Token op, Expr target, Expr value){
        this.op = op;
        this.target = target;
        this.value = value;

    }
}
