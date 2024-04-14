package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class PostfixExpr implements Expr{
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

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException {
        List<Quad> quads = new ArrayList<>();
        Symbol symbol = Symbol.findSymbol(symbolTable, literal.literal);

        Symbol loadedSymbol = Compiler.generateResultSymbol();
        symbolTable.peek().add(loadedSymbol);

        Symbol increasedSymbol = Compiler.generateResultSymbol();
        symbolTable.peek().add(increasedSymbol);

        quads.add(new Quad(QuadOp.LOAD, symbol, null, loadedSymbol));
        quads.add(new Quad(QuadOp.INC, loadedSymbol, null, increasedSymbol));
        quads.add(new Quad(QuadOp.STORE, increasedSymbol, null, symbol));
        return quads;
    }
}
