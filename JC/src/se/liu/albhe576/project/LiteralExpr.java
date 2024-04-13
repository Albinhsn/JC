package se.liu.albhe576.project;

import java.util.List;

public class LiteralExpr extends Expr{

    @Override
    public String toString() {
        if(token.type == TokenType.TOKEN_STRING){
            return String.format("\"%s\"", token.literal);
        }
        return token.literal;
    }
    private final Token token;
    public LiteralExpr(Token token){
        this.token = token;

    }
    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        return block.createImmediate(token);
    }

}

