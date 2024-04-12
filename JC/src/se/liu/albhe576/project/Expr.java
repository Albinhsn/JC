package se.liu.albhe576.project;

public class Expr {

    @Override
    public String toString() {
        return "EMPTY?";
    }

    public Expr literal(Token token){
        return new LiteralExpr(token);
    }
    public Expr identifier(Token token){
        return new VarExpr(token);
    }
    public Expr logical(Token token){
        return this;
    }
    public Expr comparison(Token token){
        return this;
    }
    public Expr minus(Token token){
        return this;
    }
    public Expr unary(Token token){
        return this;
    }
    public Expr operation(Token token){
        return this;
    }
    public Expr grouped(Token token){
        assert(token.type == TokenType.TOKEN_LEFT_PAREN);
        return new GroupedExpr(new Expr());
    }
    // Called when we want to add a grouped expression to smth
    public Expr grouped(GroupedExpr expr){
        return expr;
    }
}
