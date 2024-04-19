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
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        Symbol symbol = symbolTable.findSymbol(literal.literal);

        Symbol loadedSymbol = Compiler.generateSymbol(symbol.type);

        quads.createLoad(symbol);
        if(!loadedSymbol.type.isPointer()){
            if(op.type == TokenType.TOKEN_INCREMENT){
                quads.createIncrement(loadedSymbol);
            }else{
                quads.createDecrement(loadedSymbol);
            }
        }else{
            int structSize = symbolTable.getStructSize(loadedSymbol.type);
            quads.createMovRegisterAToC(loadedSymbol);
            Symbol loadedImmediate = quads.createLoadImmediate(DataType.getInt(), String.valueOf(structSize));

            quads.createAdd(loadedSymbol, loadedImmediate);
        }
        quads.createStore(symbol);
    }
}
