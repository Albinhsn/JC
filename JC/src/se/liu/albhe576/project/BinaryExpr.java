package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

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

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws CompileException, UnknownSymbolException {
        List<Quad> l = left.compile(symbolTable);
        Symbol lSymbol = Quad.getLastResult(l);
        List<Quad> r = right.compile(symbolTable);
        Symbol rSymbol = Quad.getLastResult(r);

        l.addAll(r);
        Symbol symbol = Compiler.generateResultSymbol();
        symbolTable.peek().add(symbol);
        l.add(new Quad(QuadOp.fromToken(op), lSymbol, rSymbol, symbol));
        return l;
    }
}
