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
        QuadOp op = (symbol.type.isStruct() || symbol.type.isArray()) ? QuadOp.LOAD_VARIABLE_POINTER : QuadOp.LOAD;
        quads.addQuad(op, symbol, null, Compiler.generateSymbol(symbol.type));
    }
}
