package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class IndexExpr implements Expr{

    @Override
    public String toString() {
        return String.format("%s[%s]", value, index);
    }

    private final Expr value;
    private final Expr index;
    public IndexExpr(Expr value, Expr index){
        this.value = value;
        this.index = index;

    }

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        List<Quad> val = value.compile(symbolTable);
        Symbol valSymbol = Quad.getLastResult(val);

        List<Quad> idx = index.compile(symbolTable);
        Symbol idxSymbol = Quad.getLastResult(idx);

        val.addAll(idx);
        val.add(new Quad(QuadOp.INDEX, valSymbol, idxSymbol, Compiler.generateResultSymbol()));
        return val;
    }
}
