package se.liu.albhe576.project;

public class AugmentedExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s %s %s", variable.literal, op.literal, expr);
    }


    private final Token op;
    private final Token variable;
    private final Expr expr;
    public AugmentedExpr(Token op, Token variable, Expr expr){
        this.op = op;
        this.variable = variable;
        this.expr = expr;

    }
}
