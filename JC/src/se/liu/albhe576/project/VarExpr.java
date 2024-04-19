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
    public VarExpr(Token token, int line, String file){
        super(line, file);
        this.token = token;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {

        // ToDo check that the variable exist?
        Symbol symbol = symbolTable.findSymbol(token.literal);
        if(symbol == null){
            this.error(String.format("Can't find symbol %s", token.literal));
        }

        assert symbol != null;
        QuadOp op = (symbol.type.isStruct() || symbol.type.isArray()) ? QuadOp.LOAD_POINTER : QuadOp.LOAD;
        quads.addQuad(op, symbol, null, Compiler.generateSymbol(symbol.type));
    }
}
