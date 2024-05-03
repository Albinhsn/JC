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
         switch(this.op.type()){
            case TOKEN_AND_BIT -> {
                Quad loaded = quads.pop();
                // ToDo TYPE CHECK?
                if(loaded.op() == QuadOp.LOAD){
                    quads.createLoadPointer(symbolTable, loaded.operand1());
                }else if(loaded.op() == QuadOp.INDEX){
                    quads.createReferenceIndex(symbolTable, loaded.operand1(), loaded.operand2());
                }else if(loaded.op() == QuadOp.LOAD_MEMBER){
                    MemberSymbol memberSymbol = (MemberSymbol) source;
                    quads.createLoadMemberPointer(symbolTable, memberSymbol, lastResult.type);
                }
            }
            case TOKEN_STAR -> quads.createDereference(symbolTable, lastResult);
            case TOKEN_MINUS -> quads.createNegate(symbolTable, lastResult);
            case TOKEN_BANG -> {
                QuadOp op = QuadOp.fromToken(this.op.type());
                quads.createLogicalNot(symbolTable, lastResult, op);
            }
            case TOKEN_INCREMENT, TOKEN_DECREMENT -> {
                lastResult = AssignStmt.setupAssignment(symbolTable, quads, lastResult);
                quads.createPrefix(lastResult, this.op.type());
            }
            default -> Compiler.error(String.format("Invalid unary op? %s", this.op.literal()), this.line, this.file);
         }
    }
}
