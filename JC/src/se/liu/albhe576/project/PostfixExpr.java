package se.liu.albhe576.project;

import java.util.List;

public class PostfixExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s%s",literal.literal, op.literal);
    }

    public Token literal;
    public Token op;

    public PostfixExpr(Token literal, Token op){
        this.literal = literal;
        this.op   = op;
    }
    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        Value value = Symbol.lookupSymbol(symbols, literal);
        return block.createPostfix(value, op);
    }
}
