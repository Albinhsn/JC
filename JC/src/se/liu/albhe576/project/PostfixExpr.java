package se.liu.albhe576.project;

public class PostfixExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s%s",expr, op.literal);
    }

    public Expr expr;
    public Token op;

    public PostfixExpr(Expr expr, Token op){
        this.expr = expr;
        this.op   = op;

    }
}
