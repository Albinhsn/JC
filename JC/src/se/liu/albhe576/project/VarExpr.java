package se.liu.albhe576.project;

public class VarExpr extends Expr {
    @Override
    public String toString() {
        return token.literal;
    }

    public final Token token;
    public VarExpr(Token token){
        this.token = token;
    }
}
