package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class PostfixExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s%s",literal.literal, op.literal);
    }

    public Token literal;
    public Token op;

    public PostfixExpr(Token literal, Token op, int line){
        super(line);
        this.literal = literal;
        this.op   = op;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException {
        QuadList quads = new QuadList();
        Symbol symbol = symbolTable.findSymbol(literal.literal);

        Symbol loadedSymbol = Compiler.generateSymbol(symbol.type);
        Symbol increasedSymbol = Compiler.generateSymbol(symbol.type);

        quads.addQuad(QuadOp.LOAD, symbol, null, loadedSymbol);
        if(!loadedSymbol.type.type.isPointer()){
            quads.addQuad(QuadOp.INC, loadedSymbol, null, increasedSymbol);
        }else{
            quads.addQuad(QuadOp.MOV_REG_CA, null, null, null);
            quads.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "8"), null, null);
            quads.addQuad(QuadOp.ADD, loadedSymbol, null, increasedSymbol);
        }
        quads.addQuad(QuadOp.STORE, increasedSymbol, null, symbol);
        return quads;
    }
}
