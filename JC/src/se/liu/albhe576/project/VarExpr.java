package se.liu.albhe576.project;

public class VarExpr extends Expr {
    public final Token token;
    public VarExpr(Token token, int line, String file){
        super(line, file);
        this.token = token;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        Symbol symbol = symbolTable.findSymbol(token.literal());
        if(symbol == null){
            Compiler.error(String.format("Can't find symbol %s", token.literal()), line, file);
        }

        assert symbol != null;
        if(!symbol.type.isArray()){
            Symbol loadedSymbol = Compiler.generateSymbol(DataType.getPointerFromType(symbol.type));
            quads.addQuad(QuadOp.LOAD_VARIABLE_POINTER, symbol, null, loadedSymbol);
            quads.addQuad(QuadOp.LOAD, loadedSymbol, null, symbol);
        }else{
            quads.addQuad(QuadOp.LOAD_VARIABLE_POINTER, symbol, null, symbol);
        }

    }
}
