package se.liu.albhe576.project;

import java.util.List;

public class BinaryExpr extends Expr{

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


    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        return block.createBinary(left.compile(functions, block, symbols), op, right.compile(functions, block, symbols));
    }
}
