package se.liu.albhe576.project;


public class UnaryExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s(%s)", op.literal, expr);
    }

    public Expr expr;
    public Token op;
    public UnaryExpr(Expr expr, Token op, int line, String file){
        super(line, file);
        this.expr = expr;
        this.op = op;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        expr.compile(symbolTable, quads);
        Symbol symbol = quads.getLastResult();
        Symbol result = Compiler.generateSymbol(symbol.type);
        QuadOp quadOp;

        switch(op.type){
            case TOKEN_AND_BIT:{
                symbol = quads.getLastOperand1();
                quads.removeLastQuad();
                result = Compiler.generateSymbol(DataType.getPointerFromType(symbol.type));
                quadOp = QuadOp.LOAD_POINTER;
                break;
            }
            // Dereference
            case TOKEN_STAR:{
                quadOp = QuadOp.DEREFERENCE;
                symbol = quads.getLastOperand1();
                result = Compiler.generateSymbol(result.type.getTypeFromPointer());
                break;
            }
            case TOKEN_MINUS:{
               quadOp = QuadOp.NOT;
               break;
            }
            case TOKEN_INCREMENT, TOKEN_DECREMENT:{
                if(result.type.isPointer()){
                    quads.createSetupUnary(symbolTable, result);

                    Symbol movedImm = Compiler.generateSymbol(DataType.getInt());

                    QuadOp op = this.op.type == TokenType.TOKEN_INCREMENT ? QuadOp.ADD : QuadOp.SUB;
                    quads.addQuad(op, Compiler.generateSymbol(result.type), movedImm, Compiler.generateSymbol(result.type));
                    return;
                }
                quadOp = QuadOp.fromToken(op);
                break;
            }
            default:{
                quadOp = QuadOp.fromToken(op);
            }
        }
        quads.addQuad(quadOp, symbol, null, result);
    }
}
