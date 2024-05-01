package se.liu.albhe576.project;

public class PostfixExpr extends Expr{
    private final Expr target;
    private final Token op;

    public PostfixExpr(Expr target, Token op, int line, String file){
        super(line, file);
        this.target = target;
        this.op   = op;
    }
    private static boolean isInvalidPostfixTarget(DataType type){return type.isArray() || type.isString() || type.isStruct();}
    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        this.target.compile(symbolTable, quads);
        Symbol target = quads.getLastResult();
        if(isInvalidPostfixTarget(target.type)){
            Compiler.error(String.format("Can't postfix type %s", target.type), line, file);
        }
        AssignStmt.setupAssignment(symbolTable, quads, target);
        quads.createPostfix(target, op.type());
    }
}
