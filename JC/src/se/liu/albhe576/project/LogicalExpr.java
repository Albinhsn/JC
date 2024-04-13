package se.liu.albhe576.project;

import java.util.List;

public class LogicalExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s %s %s", left, op.literal, right);
    }

    public Expr left;
    public Expr right;
    public Token op;
    public LogicalExpr(Expr left, Expr right, Token op){
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        return block.createBinary(left.compile(functions, block, symbols), op, right.compile(functions, block, symbols));
    }
}
