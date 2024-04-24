package se.liu.albhe576.project;


public class UnaryExpr extends Expr{
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
        Symbol lastResult = quads.getLastResult();
        Symbol result = Compiler.generateSymbol(lastResult.type);
        QuadOp quadOp = this.getUnaryQuadOp();

        // ToDo Type check that it isn't an immediate?

        // Take reference (&foo)
        if(quadOp == QuadOp.LOAD_POINTER){
            Quad lastQuad = quads.pop();
            QuadOp lastOp = lastQuad.op();

            if(lastOp == QuadOp.GET_FIELD){
                Symbol op2 = lastQuad.operand2();
                result = Compiler.generateSymbol(DataType.getPointerFromType(op2.type));
                quads.addQuad(QuadOp.LOAD_FIELD_POINTER, quads.getLastOperand1(), op2, result);
            }else if(lastOp == QuadOp.INDEX){
                quads.addQuad(quadOp, lastResult, null, Compiler.generateSymbol(DataType.getPointerFromType(lastQuad.operand2().type)));
            }
            else{
                Symbol op1 = lastQuad.operand1();
                result = Compiler.generateSymbol(DataType.getPointerFromType(op1.type));
                quads.addQuad(QuadOp.LOAD_VARIABLE_POINTER, op1, null, result);
            }

        }
        // Dereference
        else if(quadOp == QuadOp.DEREFERENCE){
            quads.addQuad(quadOp, quads.getLastOperand1(), null, Compiler.generateSymbol(result.type.getTypeFromPointer()));
        }
        // inc/dec
        else if((quadOp == QuadOp.ADD || quadOp == QuadOp.SUB) && result.type.isPointer()){
            quads.createSetupUnary(symbolTable, result);
            Symbol movedImm = Compiler.generateSymbol(DataType.getInt());
            quads.addQuad(quadOp, Compiler.generateSymbol(result.type), movedImm, Compiler.generateSymbol(result.type));
        }else{
            quads.addQuad(quadOp, lastResult, null, Compiler.generateSymbol(lastResult.type));
        }
    }
}
