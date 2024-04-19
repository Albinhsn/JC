package se.liu.albhe576.project;

public class PostfixExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s%s",literal.literal, op.literal);
    }

    public Token literal;
    public Token op;

    public PostfixExpr(Token literal, Token op, int line, String file){
        super(line, file);
        this.literal = literal;
        this.op   = op;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException {
        Symbol symbol = symbolTable.findSymbol(literal.literal);

        Symbol loadedSymbol = Compiler.generateSymbol(symbol.type);
        Symbol increasedSymbol = Compiler.generateSymbol(symbol.type);

        quads.addQuad(QuadOp.LOAD, symbol, null, loadedSymbol);
        if(!loadedSymbol.type.isPointer()){
            quads.addQuad(QuadOp.INC, loadedSymbol, null, increasedSymbol);
        }else{
            int structSize = symbolTable.getStructSize(loadedSymbol.type);
            quads.addQuad(QuadOp.MOV_REG_CA, loadedSymbol, null, null);
            quads.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(structSize)), null, null);
            quads.addQuad(QuadOp.ADD, loadedSymbol, null, increasedSymbol);
        }
        quads.addQuad(QuadOp.STORE, increasedSymbol, null, symbol);
    }
}
