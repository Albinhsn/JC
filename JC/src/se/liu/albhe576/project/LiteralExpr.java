package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class LiteralExpr extends Expr{

    @Override
    public String toString() {
        if(token.type == TokenType.TOKEN_STRING){
            return String.format("\"%s\"", token.literal);
        }
        return token.literal;
    }
    private final Token token;
    public LiteralExpr(Token token, int line, String file){
        super(line, file);
        this.token = token;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnexpectedTokenException {
        DataType type = DataType.getDataTypeFromToken(token);
        Symbol immediateSymbol = Compiler.generateImmediateSymbol(type, token.literal);

        Symbol resultSymbol = Compiler.generateSymbol(type);
        switch(token.type){
            case TOKEN_STRING:{}
            case TOKEN_FLOAT_LITERAL:{
                symbolTable.addConstant(token.literal, type.type);
                break;
            }
            default :{}
        }

        quads.addQuad(QuadOp.LOAD_IMM, immediateSymbol, null, resultSymbol);
    }
}

