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
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException {

        // ToDo check that the variable exist?
        Symbol symbol = symbolTable.findSymbol(token.literal);
        if(symbol == null){
            throw new UnknownSymbolException(String.format("Can't find symbol %s at line %s", token.literal, line));
        }

        QuadOp op = (symbol.type.isStruct() || symbol.type.isArray()) ? QuadOp.LOAD_POINTER : QuadOp.LOAD;
        quads.addQuad(op, symbol, null, Compiler.generateSymbol(symbol.type));
    }
}
