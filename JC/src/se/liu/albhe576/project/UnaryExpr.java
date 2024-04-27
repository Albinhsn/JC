package se.liu.albhe576.project;


public class UnaryExpr extends Expr{
    private final Expr expr;
    private final Token op;
    public UnaryExpr(Expr expr, Token op, int line, String file){
        super(line, file);
        this.expr = expr;
        this.op = op;
    }
    private void unaryReference(QuadList quads) {
        if(quads.getLastResult().type.isArray()){
            ArrayDataType arrayDataType = (ArrayDataType) quads.getLastResult().type;
            Quad lastQuad = quads.pop();
            DataType pointerType = DataType.getPointerFromType(arrayDataType.itemType);
            quads.addQuad(lastQuad.op(), lastQuad.operand1(), lastQuad.operand2(), Compiler.generateSymbol(pointerType));
        }else{
            quads.removeLastQuad();
        }
    }
    private void compileDereference(QuadList quads, Symbol lastResult) throws CompileException {
        if(!lastResult.type.isPointer()){
            Compiler.error("Can't dereference none pointer", this.line, this.file);
        }
        quads.addQuad(QuadOp.LOAD, quads.getLastOperand1(), null, Compiler.generateSymbol(lastResult.type.getTypeFromPointer()));
    }
    private void compileNegate(QuadList quads, Symbol lastResult) throws CompileException {
        if(!(lastResult.type.isInteger())){
            Compiler.error("Can't negate non int/byte", this.line, this.file);
        }
        quads.addQuad(QuadOp.NEGATE, lastResult, null, lastResult);
    }
    private void compileIncrementAndDecrement(SymbolTable symbolTable, QuadList quads, Symbol lastResult, Token tokenOp) throws CompileException {
        Quad lastQuad = quads.getLastQuad();

        Symbol res = Compiler.generateSymbol(lastResult.type);
        if(lastResult.type.isPointer()){
            QuadOp op =  tokenOp.type() == TokenType.TOKEN_INCREMENT ? QuadOp.ADDI : QuadOp.SUBI;
            int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), lastResult.type.getTypeFromPointer());
            quads.addQuad(op, Compiler.generateImmediateSymbol(DataType.getLong(), String.valueOf(structSize)), null, lastResult);
        }else{
            QuadOp op = tokenOp.type() == TokenType.TOKEN_INCREMENT ? QuadOp.INC : QuadOp.DEC;
            quads.addQuad(op, Compiler.generateSymbol(lastResult.type), null, Compiler.generateSymbol(lastResult.type));
        }
        quads.createPush(res);
        this.expr.compile(symbolTable, quads);
        quads.removeLastQuad();
        quads.createMovRegisterAToC(quads.getLastResult());
        Symbol popped = quads.createPop(res);
        quads.addQuad(QuadOp.STORE, popped, null, popped);
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        expr.compile(symbolTable, quads);
        Symbol lastResult = quads.getLastResult();

        switch(this.op.type()){
            case TOKEN_AND_BIT -> this.unaryReference(quads);
            case TOKEN_STAR -> this.compileDereference(quads, lastResult);
            case TOKEN_MINUS -> this.compileNegate(quads, lastResult);
            case TOKEN_INCREMENT, TOKEN_DECREMENT -> this.compileIncrementAndDecrement(symbolTable, quads, lastResult, op);
            default -> Compiler.error(String.format("Invalid unary op? %s", this.op.literal()), this.line, this.file);
        }
    }
}
