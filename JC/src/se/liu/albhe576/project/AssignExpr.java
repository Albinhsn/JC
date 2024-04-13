package se.liu.albhe576.project;

import java.util.List;

public class AssignExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s = %s",variableExpr, value);
    }
    private final Expr variableExpr;
    private final Expr value;

    public AssignExpr(Expr variableExpr, Expr value){
        this.variableExpr = variableExpr;
        this.value = value;
    }

    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {

        Value target = variableExpr.compile(functions, block, symbols);
        Value value = this.value.compile(functions, block, symbols);
        return block.createStore(symbols, target, value);
    }
}
