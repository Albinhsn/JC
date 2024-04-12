package se.liu.albhe576.project;

public class UnaryExpr {
    public Expr expr;
    public Token op;
    public UnaryExpr(Expr expr, Token op){
        this.expr = expr;
        this.op = op;
    }
}
