package se.liu.albhe576.project;

import java.util.List;

public class IndexExpr extends Expr{

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
    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        Value target = value.compile(functions, block, symbols);
        Value indexValue = index.compile(functions, block, symbols);
        return block.createIndex(target, indexValue);
    }
}
