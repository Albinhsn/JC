package se.liu.albhe576.project;


import java.util.List;
import java.util.Stack;

public class UnaryExpr implements Expr{
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

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        List<Quad> quads = expr.compile(symbolTable);
        Symbol symbol =Quad.getLastResult(quads);

        quads.add(new Quad(QuadOp.fromToken(op), symbol, null, Compiler.generateResultSymbol()));
        return quads;
    }
}
