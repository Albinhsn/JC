package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class VarExpr extends Expr {
    @Override
    public String toString() {
        return token.literal;
    }

    public final Token token;
    public VarExpr(Token token, int line){
        super(line);
        this.token = token;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException {

        QuadList quads = new QuadList();

        Symbol symbol = symbolTable.findSymbol(token.literal);
        if(symbol.type.type == DataTypes.STRUCT){
            quads.addQuad(QuadOp.LOAD_POINTER, symbol, null, Compiler.generateSymbol(symbol.type));
            return quads;
        }

        quads.addQuad(QuadOp.LOAD, symbol, null, Compiler.generateSymbol(symbol.type));
        return quads;
    }
}
