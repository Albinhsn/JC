package se.liu.albhe576.project;


public class UnaryExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s%s", op.literal, expr);
    }

    public Expr expr;
    public Token op;
    public UnaryExpr(Expr expr, Token op, int line){
        super(line);
        this.expr = expr;
        this.op = op;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        QuadList quads = expr.compile(symbolTable);
        Symbol symbol = quads.getLastResult();
        Symbol result = Compiler.generateSymbol(symbol.type);
        QuadOp quadOp;

        switch(op.type){
            // Take the address of a pointer
            // int foo = &bar;
            case TOKEN_AND_BIT:{
                // Remove load
                symbol = quads.getLastOperand1();
                quads.removeLastQuad();
                result = Compiler.generateSymbol(DataType.getPointerFromType(symbol.type));
                quadOp = QuadOp.LOAD_POINTER;
                break;
            }
            // Dereference
            case TOKEN_STAR:{
                quadOp = QuadOp.DEREFERENCE;
                Symbol operand = quads.getLastOperand1();
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

        quads.addQuad(quadOp, symbol, null, result);
        return quads;
    }
}
