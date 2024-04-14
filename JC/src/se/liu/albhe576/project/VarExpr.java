package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class VarExpr implements  Expr {
    @Override
    public String toString() {
        return token.literal;
    }

    public final Token token;
    public VarExpr(Token token){
        this.token = token;
    }

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException {
        List<Quad> quads = new ArrayList<>();
        ResultSymbol result = Compiler.generateResultSymbol();
        symbolTable.peek().add(result);
        quads.add(new Quad(QuadOp.LOAD, Symbol.findSymbol(symbolTable, token.literal), null, result));
        return quads;
    }
}
