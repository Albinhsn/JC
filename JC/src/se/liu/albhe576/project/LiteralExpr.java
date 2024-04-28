package se.liu.albhe576.project;

public class LiteralExpr extends Expr{
    private final Token token;
    public LiteralExpr(Token token, int line, String file){
        super(line, file);
        this.token = token;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
        DataType type = DataType.getDataTypeFromToken(token);

        if(token.type() == TokenType.TOKEN_STRING_LITERAL || token.type() == TokenType.TOKEN_FLOAT_LITERAL){
            symbolTable.addConstant(token.literal(), type.type);
        }

        quads.createLoadImmediate(type, token.literal());
    }
}

