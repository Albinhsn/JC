package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class AugmentedExpr implements  Expr{

    @Override
    public String toString() {
        return String.format("%s %s %s", target, op.literal, value);
    }


    private final Token op;
    private final Expr target;
    private final Expr value;
    public AugmentedExpr(Token op, Expr target, Expr value){
        this.op = op;
        this.target = target;
        this.value = value;

    }

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws CompileException, UnknownSymbolException {
        List<Quad> targetQuads = target.compile(symbolTable);
        Symbol targetSymbol = targetQuads.get(targetQuads.size() - 1).result;
        List<Quad> valueQuads = value.compile(symbolTable);
        Symbol valueSymbol = valueQuads.get(valueQuads.size() - 1).result;


        targetQuads.addAll(valueQuads);
        targetQuads.add(new Quad(QuadOp.fromToken(op), valueSymbol, null, targetSymbol));

        return targetQuads;
    }
}
