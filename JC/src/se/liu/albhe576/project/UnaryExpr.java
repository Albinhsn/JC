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
    private void unaryReference(QuadList quads, Symbol lastResult){
        Quad lastQuad = quads.pop();
        QuadOp lastOp = lastQuad.op();

        if(lastOp == QuadOp.LOAD_IMM){
            Compiler.error("Can't take address of immediate?", this.line, this.file);
        }

        if(lastOp == QuadOp.GET_FIELD){
            Symbol op2 = lastQuad.operand2();
            lastResult = Compiler.generateSymbol(DataType.getPointerFromType(op2.type));
            quads.addQuad(QuadOp.LOAD_FIELD_POINTER, quads.getLastOperand1(), op2, lastResult);
        }else if(lastOp == QuadOp.INDEX){
            quads.addQuad(QuadOp.LOAD_POINTER, lastResult, null, Compiler.generateSymbol(DataType.getPointerFromType(lastQuad.operand2().type)));
        }
        else{
            Symbol op1 = lastQuad.operand1();
            lastResult = Compiler.generateSymbol(DataType.getPointerFromType(op1.type));
            quads.addQuad(QuadOp.LOAD_VARIABLE_POINTER, op1, null, lastResult);
        }
    }
    private void unaryDereference(QuadList quads, Symbol lastResult) throws CompileException {
        if(!lastResult.type.isPointer()){
            Compiler.error("Can't dereference none pointer", this.line, this.file);
        }
        quads.addQuad(QuadOp.DEREFERENCE, quads.getLastOperand1(), null, Compiler.generateSymbol(lastResult.type.getTypeFromPointer()));
    }
    private void unaryNegate(QuadList quads, Symbol lastResult){
        if(!(lastResult.type.isInteger() || lastResult.type.isByte())){
            Compiler.error("Can't negate non int/byte", this.line, this.file);
        }
        quads.addQuad(QuadOp.NEGATE, lastResult, null, lastResult);
    }
    private void unaryPrefix(SymbolTable symbolTable, QuadList quads, Symbol lastResult, QuadOp op) throws CompileException {
        Quad lastQuad = quads.getLastQuad();

        Symbol movedImm = null;
        if(lastResult.type.isPointer()){
            quads.createSetupUnary(symbolTable, lastResult);
            movedImm = Compiler.generateSymbol(DataType.getInt());
            op = op == QuadOp.INC ? QuadOp.ADD : QuadOp.SUB;
        }
        Symbol res = Compiler.generateSymbol(lastResult.type);

        quads.addQuad(op, Compiler.generateSymbol(lastResult.type), movedImm, res);

        switch(lastQuad.op()){
            case LOAD -> {
                quads.createPush(res);
                quads.createStore(lastQuad.operand1());
                quads.createPop(res);
            }
            case INDEX -> {
                QuadList index = new QuadList();
                this.expr.compile(symbolTable, index);
                AssignStmt.compileStoreIndex(quads, index);
            }
            case GET_FIELD -> {
                QuadList index = new QuadList();
                this.expr.compile(symbolTable, index);
                AssignStmt.compileStoreField(quads, index);
            }
            case DEREFERENCE -> {
                QuadList index = new QuadList();
                this.expr.compile(symbolTable, index);
                AssignStmt.compileStoreDereferenced(quads, index);
            }

        }



    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        expr.compile(symbolTable, quads);
        Symbol lastResult = quads.getLastResult();

        QuadOp op = this.getUnaryQuadOp();
        switch(op){
            case LOAD_POINTER -> this.unaryReference(quads,lastResult);
            case DEREFERENCE -> this.unaryDereference(quads, lastResult);
            case NEGATE -> this.unaryNegate(quads, lastResult);
            case INC, DEC -> this.unaryPrefix(symbolTable, quads, lastResult, op);
            default -> Compiler.error(String.format("Invalid unary op? %s", this.getUnaryQuadOp().name()), this.line, this.file);
        }
    }
}
