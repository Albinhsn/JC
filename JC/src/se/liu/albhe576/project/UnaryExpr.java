package se.liu.albhe576.project;


public class UnaryExpr extends Expr{
    private final Expr expr;
    private final Token op;
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
        Symbol op1 = quads.getLastResult();
        Symbol op2 = null;
        Symbol result = Compiler.generateSymbol(op1.type);
        QuadOp quadOp = this.getUnaryQuadOp();

        // ToDo Type check that it isn't an immediate?

        // Take reference (&foo)
        if(quadOp == QuadOp.LOAD_POINTER){
            Quad lastQuad = quads.pop();
            QuadOp lastOp = lastQuad.getOp();

            if(lastOp == QuadOp.GET_FIELD){
                // load field pointer?

                quadOp = QuadOp.LOAD_FIELD_POINTER;
                op2 = lastQuad.getOperand2();
                op1 = quads.getLastOperand1();
                result = Compiler.generateSymbol(DataType.getPointerFromType(op2.type));
            }else if(lastOp == QuadOp.INDEX){
                result = Compiler.generateSymbol(DataType.getPointerFromType(lastQuad.getOperand2().type));
            }
            else{
                quadOp = QuadOp.LOAD_VARIABLE_POINTER;
                op1 = lastQuad.getOperand1();
                result = Compiler.generateSymbol(DataType.getPointerFromType(op1.type));
            }
        }
        // Dereference
        else if(quadOp == QuadOp.DEREFERENCE){
            op1 = quads.getLastOperand1();
            result = Compiler.generateSymbol(result.type.getTypeFromPointer());
        }
        // inc/dec
        else if((quadOp == QuadOp.ADD || quadOp == QuadOp.SUB) && result.type.isPointer()){
            quads.createSetupUnary(symbolTable, result);
            Symbol movedImm = Compiler.generateSymbol(DataType.getInt());
            quads.addQuad(quadOp, Compiler.generateSymbol(result.type), movedImm, Compiler.generateSymbol(result.type));
            return;
        }

        quads.addQuad(quadOp, op1, op2, result);
    }
}
