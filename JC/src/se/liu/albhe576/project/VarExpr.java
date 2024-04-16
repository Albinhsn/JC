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
    public List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException {

        List<Quad> quads = new ArrayList<>();

        Symbol symbol = symbolTable.findSymbol(token.literal);
        if(symbol.type.type == DataTypes.STRUCT){
            quads.add(new Quad(QuadOp.LOAD_POINTER, symbol, null, Compiler.generateSymbol(symbol.type)));
            return quads;
        }

        quads.add(new Quad(QuadOp.LOAD, symbol, null, Compiler.generateSymbol(symbol.type)));
        return quads;
    }
}
