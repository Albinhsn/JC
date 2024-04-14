package se.liu.albhe576.project;

import java.util.List;

public class BinaryExpr implements Expr{

    public Expr left;
    public Expr right;
    public Token op;

    @Override
    public String toString() {
        return String.format("(%s %s %s)", left, op.literal, right);
    }



    public BinaryExpr(Expr left, Token op, Expr right){
        this.left = left;
        this.op = op;
        this.right = right;
    }
}
