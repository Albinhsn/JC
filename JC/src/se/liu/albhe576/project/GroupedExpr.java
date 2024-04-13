package se.liu.albhe576.project;

import java.util.List;

public class GroupedExpr extends Expr{
    @Override
    public String toString() {
        return String.format("(%s)", expr);
    }

    private Expr expr;
    public GroupedExpr(Expr expr){
        this.expr = expr;
    }
    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        return expr.compile(functions, block, symbols);
    }

}
