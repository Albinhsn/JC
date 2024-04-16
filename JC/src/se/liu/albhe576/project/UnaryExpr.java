package se.liu.albhe576.project;


import java.util.List;
import java.util.Stack;

public class UnaryExpr implements Expr{
    @Override
    public String toString() {
        return String.format("%s%s", op.literal, expr);
    }

    public Expr expr;
    public Token op;
    public UnaryExpr(Expr expr, Token op){
        this.expr = expr;
        this.op = op;
    }

    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        List<Quad> quads = expr.compile(symbolTable);
        Symbol symbol = Quad.getLastResult(quads);
        Symbol result = Compiler.generateSymbol(symbol.type);
        QuadOp quadOp;

        switch(op.type){
            // Take the address of a pointer
            case TOKEN_AND_BIT:{
                // Remove load
                symbol = Quad.getLastOperand1(quads);
                quads.remove(quads.size() - 1);
                result = Compiler.generateSymbol(DataType.getPointerFromType(symbol.type));
                quadOp = QuadOp.LOAD_POINTER;
                break;
            }
            // Dereference
            case TOKEN_STAR:{
                quadOp = QuadOp.DEREFERENCE;
                Symbol operand = Quad.getLastOperand1(quads);
                result = Compiler.generateSymbol(DataType.getTypeFromPointer(operand.type));
                break;
            }
            case TOKEN_MINUS:{
               quadOp = QuadOp.NOT;
               break;
            }
            default:{
                quadOp = QuadOp.fromToken(op);
            }
        }

        quads.add(new Quad(quadOp, symbol, null, result));
        return quads;
    }
}
