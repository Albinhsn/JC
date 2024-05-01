package se.liu.albhe576.project;


public class UnaryExpr extends Expr{
    private final Expr expr;
    private final Token op;
    public UnaryExpr(Expr expr, Token op, int line, String file){
        super(line, file);
        this.expr = expr;
        this.op = op;
    }
    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        expr.compile(symbolTable, quads);
        Symbol source = quads.getLastOperand1();
        Symbol lastResult = quads.getLastResult();
        Symbol lastOperand2 = quads.getLastOperand2();
        QuadOp lastOp = quads.getLastOp();
         switch(this.op.type()){
            case TOKEN_AND_BIT -> {
                Quad loaded = quads.pop();
                // TYPE CHECK?
                if(loaded.op() == QuadOp.LOAD_I || loaded.op() == QuadOp.LOAD_F){
                    quads.createLoadPointer(loaded.operand1());
                }else if(loaded.op() == QuadOp.INDEX){
                    quads.createReferenceIndex(loaded.operand1(), loaded.operand2());
                }
            }
            case TOKEN_STAR -> quads.createDereference(lastResult);
            case TOKEN_MINUS -> quads.createNegate(lastResult);
            case TOKEN_INCREMENT, TOKEN_DECREMENT -> {
                quads.pop();
                if(lastOp == QuadOp.LOAD_I || lastOp == QuadOp.LOAD_F){
                    quads.createLoadPointer(source);
                }else if(lastOp == QuadOp.LOAD_MEMBER){
                    quads.createLoadMemberPointer(source, lastOperand2, lastResult.type);
                }else if(lastOp == QuadOp.INDEX){
                    quads.createReferenceIndex(source, lastOperand2);
                    lastResult = Compiler.generateSymbol(source.type.getTypeFromPointer());
                }
                quads.createPrefix(lastResult, this.op.type());
            }
            default -> Compiler.error(String.format("Invalid unary op? %s", this.op.literal()), this.line, this.file);
         }
    }
}
