package se.liu.albhe576.project;

import java.util.List;

public class VarExpr implements  Expr {
    @Override
    public String toString() {
        return token.literal;
    }

    public final Token token;
    public VarExpr(Token token){
        this.token = token;
    }

}
