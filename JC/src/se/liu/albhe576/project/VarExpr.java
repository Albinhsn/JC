package se.liu.albhe576.project;

public class VarExpr extends Expr {
    public final Token token;
    public VarExpr(Token token, int line, String file){
        super(line, file);
        this.token = token;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        VariableSymbol symbol = symbolTable.findSymbol(token.literal());
        if(symbol == null){
            Compiler.error(String.format("Can't find symbol %s", token.literal()), line, file);
        }

        assert symbol != null;
        quads.createLoad(symbol);
    }
}
