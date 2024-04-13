package se.liu.albhe576.project;


import java.util.List;

public class UnaryExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s%s", op.literal, expr);
    }

    public Expr expr;
    public Token op;
    public UnaryExpr(Expr expr, Token op){
        this.expr = expr;
        this.op = op;
    }
    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        return block.createUnary(expr.compile(functions, block, symbols), op);
    }
}
