package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class LogicalExpr implements Expr{

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

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        List<Quad> l = left.compile(symbolTable);
        List<Quad> r = right.compile(symbolTable);

        Symbol lSymbol = Quad.getLastResult(l);
        Symbol rSymbol = Quad.getLastResult(r);

        l.addAll(r);
        l.add(new Quad(QuadOp.fromToken(op), lSymbol, rSymbol, Compiler.generateResultSymbol()));
        return l;
    }
}
