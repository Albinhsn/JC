package se.liu.albhe576.project;


public class UnaryExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s(%s)", op.literal(), expr);
    }

    public Expr expr;
    public Token op;
    public UnaryExpr(Expr expr, Token op, int line, String file){
        super(line, file);
        this.expr = expr;
        this.op = op;
    }

    private QuadOp getUnaryQuadOp() throws CompileException {
       switch(this.op.type()){
           case TOKEN_AND_BIT -> {return QuadOp.LOAD_POINTER;}
           case TOKEN_STAR -> {return QuadOp.DEREFERENCE;}
           case TOKEN_MINUS -> {return QuadOp.NEGATE;}
           case TOKEN_INCREMENT, TOKEN_DECREMENT -> {return QuadOp.fromToken(this.op);}
       }
       throw new CompileException(String.format("Can't get quad op from unary op %s", this.op.literal()));
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        expr.compile(symbolTable, quads);
        Symbol symbol = quads.getLastResult();
        Symbol result = Compiler.generateSymbol(symbol.type);
        QuadOp quadOp = this.getUnaryQuadOp();

        // ToDo Type check that it isn't an immediate

        // Take reference
        if(quadOp == QuadOp.LOAD_POINTER){
            symbol = quads.getLastOperand1();
            quads.removeLastQuad();
            result = Compiler.generateSymbol(DataType.getPointerFromType(symbol.type));
        }
        // Dereference
        else if(quadOp == QuadOp.DEREFERENCE){
            symbol = quads.getLastOperand1();
            result = Compiler.generateSymbol(result.type.getTypeFromPointer());
        }
        // inc/dec
        else if((quadOp == QuadOp.ADD || quadOp == QuadOp.SUB) && result.type.isPointer()){
            quads.createSetupUnary(symbolTable, result);

            Symbol movedImm = Compiler.generateSymbol(DataType.getInt());

            // LOOK AT THIS
            quads.addQuad(quadOp, Compiler.generateSymbol(result.type), movedImm, Compiler.generateSymbol(result.type));
            return;
        }

        quads.addQuad(quadOp, symbol, null, result);
    }
}
