package se.liu.albhe576.project;

public class BinaryExpr extends Expr{

    @Override
    public String toString() {
        return String.format("(%s %s %s)", left, op.literal, right);
    }

    public Expr left;
    public Expr right;
    public Token op;

    public BinaryExpr(Expr left, Token op, Expr right){
        this.left = left;
        this.op = op;
        this.right = right;
    }
}
