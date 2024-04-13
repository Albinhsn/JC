package se.liu.albhe576.project;

import java.util.List;

public class AugmentedExpr extends Expr{

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
    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        Value target = this.target.compile(functions, block, symbols);
        Value targetValue = block.createLookup(symbols, target);

        Value value = this.value.compile(functions, block, symbols);

        Value newValue = block.createBinary(targetValue, op, value);
        return block.createStore(symbols, target, newValue);
    }
}
