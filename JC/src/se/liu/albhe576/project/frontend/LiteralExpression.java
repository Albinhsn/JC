package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.*;
/**
 * The basic expression for a literal, which is either a int (decimal and hex supported), float or string
 * @see Expression
 */
public class LiteralExpression extends Expression
{
    private final Token token;
    public LiteralExpression(Token token, int line, String file){
        super(line, file);
        this.token = token;
    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException{
        DataType type = DataType.getDataTypeFromToken(token);

        if(token.type() == TokenType.TOKEN_STRING_LITERAL || token.type() == TokenType.TOKEN_FLOAT_LITERAL){
            symbolTable.addConstant(token.literal(), type.getType());
        }

        intermediates.createLoadImmediate(symbolTable, symbolTable.generateImmediateSymbol(type, token.literal()));
    }
}

