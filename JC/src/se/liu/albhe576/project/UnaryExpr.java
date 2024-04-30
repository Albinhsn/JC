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
        Symbol lastResult = quads.getLastResult();
         switch(this.op.type()){
            case TOKEN_AND_BIT -> quads.createReference(lastResult);
            case TOKEN_STAR -> quads.createDereference(lastResult);
            case TOKEN_MINUS -> quads.createNegate(lastResult);
            case TOKEN_INCREMENT, TOKEN_DECREMENT -> {
                quads.createPostfix(lastResult, this.op.type());
                quads.createStore(lastResult, quads.getLastResult());
            }
            default -> Compiler.error(String.format("Invalid unary op? %s", this.op.literal()), this.line, this.file);
         }
    }
}
