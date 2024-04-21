package se.liu.albhe576.project;

public class LiteralExpr extends Expr{

    @Override
    public String toString() {
        if(token.type() == TokenType.TOKEN_STRING){
            return String.format("\"%s\"", token.literal());
        }
        return token.literal();
    }
    private final Token token;
    public LiteralExpr(Token token, int line, String file){
        super(line, file);
        this.token = token;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
        DataType type = DataType.getDataTypeFromToken(token);

        if(token.type() == TokenType.TOKEN_STRING || token.type() == TokenType.TOKEN_FLOAT_LITERAL){
            symbolTable.addConstant(token.literal(), type.type);
        }

        quads.createLoadImmediate(type, token.literal());
    }
}

