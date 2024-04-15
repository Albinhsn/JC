package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class GroupedExpr implements Expr{
    @Override
    public String toString() {
        return String.format("(%s)", expr);
    }

    private final Expr expr;
    public GroupedExpr(Expr expr){
        this.expr = expr;
    }

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        return expr.compile(symbolTable);
    }
}
