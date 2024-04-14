package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class LiteralExpr implements Expr{

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

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) {
        List<Quad> quad = new ArrayList<>();
        ResultSymbol resultSymbol = Compiler.generateResultSymbol();
        symbolTable.peek().add(resultSymbol);
        quad.add(new Quad(QuadOp.LOAD_IMM, new ImmediateSymbol(token), null, resultSymbol));
        return quad;
    }
}

