package se.liu.albhe576.project;

public class LiteralExpr extends Expr{

    @Override
    public String toString() {
        if(token.type == TokenType.TOKEN_STRING){
            return String.format("\"%s\"", token.literal);
        }
        return token.literal;
    }

    private final Token token;
    public LiteralExpr(Token token){
        this.token = token;

    }
}
