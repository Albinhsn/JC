package se.liu.albhe576.project;

import java.util.List;

public class PostfixExpr implements Expr{
    @Override
    public String toString() {
        return String.format("%s%s",literal.literal, op.literal);
    }

    public Token literal;
    public Token op;

    public PostfixExpr(Token literal, Token op){
        this.literal = literal;
        this.op   = op;
    }
}
