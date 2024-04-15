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
        Symbol targetSymbol = Quad.getLastOperand1(targetQuads);
        List<Quad> valueQuads = value.compile(symbolTable);
        Symbol valueSymbol = Quad.getLastResult(valueQuads);


        targetQuads.addAll(valueQuads);
        targetQuads.add(new Quad(QuadOp.fromToken(op), valueSymbol, null, targetSymbol));
        targetQuads.add(new Quad(QuadOp.STORE, null, null, targetSymbol));

        return targetQuads;
    }
}
