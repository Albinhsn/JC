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
    private void unaryReference(QuadList quads, Symbol lastResult) throws CompileException {
        Quad lastQuad = quads.pop();

        switch (lastQuad.op()){
            case GET_FIELD -> {
                Symbol op2 = lastQuad.operand2();
                lastResult = Compiler.generateSymbol(DataType.getPointerFromType(op2.type));
                quads.addQuad(QuadOp.LOAD_FIELD_POINTER, quads.getLastOperand1(), op2, lastResult);
            }
            case INDEX -> quads.addQuad(QuadOp.LOAD_POINTER, lastResult, null, Compiler.generateSymbol(DataType.getPointerFromType(lastQuad.operand2().type)));
            case LOAD_IMM -> Compiler.error("Can't take address of immediate?", this.line, this.file);
            default -> {
                Symbol op1 = lastQuad.operand1();
                lastResult = Compiler.generateSymbol(DataType.getPointerFromType(op1.type));
                quads.addQuad(QuadOp.LOAD_VARIABLE_POINTER, op1, null, lastResult);
            }
        }
    }
    private void compileDereference(QuadList quads, Symbol lastResult) throws CompileException {
        if(!lastResult.type.isPointer()){
            Compiler.error("Can't dereference none pointer", this.line, this.file);
        }
        quads.addQuad(QuadOp.DEREFERENCE, quads.getLastOperand1(), null, Compiler.generateSymbol(lastResult.type.getTypeFromPointer()));
    }
    private void compileNegate(QuadList quads, Symbol lastResult) throws CompileException {
        // ToDo hoist
        if(!(lastResult.type.isInteger() || lastResult.type.isByte() || lastResult.type.isShort() || lastResult.type.isLong())){
            Compiler.error("Can't negate non int/byte", this.line, this.file);
        }
        quads.addQuad(QuadOp.NEGATE, lastResult, null, lastResult);
    }
    private void compileIncrementAndDecrement(SymbolTable symbolTable, QuadList quads, Symbol lastResult, QuadOp op) throws CompileException {
        Quad lastQuad = quads.getLastQuad();

        Symbol res = Compiler.generateSymbol(lastResult.type);
        Symbol movedImm = null;
        if(lastResult.type.isPointer()){
            op = op == QuadOp.INC ? QuadOp.ADD : QuadOp.SUB;
            QuadList immQuads = new QuadList();
            int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), lastResult.type.getTypeFromPointer());
            movedImm = immQuads.createLoadImmediate(DataType.getInt(), String.valueOf(structSize));
            quads.createSetupBinary(immQuads, lastResult, movedImm);
        }

        quads.addQuad(op, Compiler.generateSymbol(lastResult.type), movedImm, Compiler.generateSymbol(lastResult.type));
        quads.createPush(res);
        AssignStmt.createStore(symbolTable, this.expr, quads, lastQuad);
        quads.createPop(res);
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        expr.compile(symbolTable, quads);
        Symbol lastResult = quads.getLastResult();

        QuadOp op = this.getUnaryQuadOp();
        switch(op){
            case LOAD_POINTER -> this.unaryReference(quads,lastResult);
            case DEREFERENCE -> this.compileDereference(quads, lastResult);
            case NEGATE -> this.compileNegate(quads, lastResult);
            case INC, DEC -> this.compileIncrementAndDecrement(symbolTable, quads, lastResult, op);
            default -> Compiler.error(String.format("Invalid unary op? %s", this.getUnaryQuadOp().name()), this.line, this.file);
        }
    }
}
