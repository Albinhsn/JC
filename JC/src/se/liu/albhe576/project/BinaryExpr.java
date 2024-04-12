package se.liu.albhe576.project;

public class BinaryExpr extends Expr{
    public Expr left;
    public Expr right;
    public TokenType op;

    public BinaryExpr(Expr left, TokenType op, Expr right){
        this.left = left;
        this.op = op;
        this.right = right;
    }
}
