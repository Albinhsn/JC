package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class LiteralExpr implements Expr{

    @Override
    public String toString() {
        if(token.type == TokenType.TOKEN_STRING){
            return String.format("\"%s\"", token.literal);
        }
        return token.literal;
    }
    private final Token token;
    public LiteralExpr(Token token){
        this.token = token;

    }

    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws UnexpectedTokenException {
        List<Quad> quad = new ArrayList<>();


        DataType type = DataType.getDataTypeFromToken(token);
        Symbol immediateSymbol = Compiler.generateImmediateSymbol(type, token.literal);

        Symbol resultSymbol = Compiler.generateSymbol(type);
        switch(token.type){
            case TOKEN_STRING:{}
            case TOKEN_FLOAT_LITERAL:{
                symbolTable.addConstant(token.literal);
                break;
            }
            default :{}
        }

        quad.add(new Quad(QuadOp.LOAD_IMM, immediateSymbol, null, resultSymbol));
        return quad;
    }
}

