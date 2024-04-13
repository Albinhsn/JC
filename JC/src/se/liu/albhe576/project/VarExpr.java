package se.liu.albhe576.project;

import java.util.List;

public class VarExpr extends Expr {
    @Override
    public String toString() {
        return token.literal;
    }

    public final Token token;
    public VarExpr(Token token){
        this.token = token;
    }

    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        return Symbol.lookupSymbol(symbols, token);
    }
}
