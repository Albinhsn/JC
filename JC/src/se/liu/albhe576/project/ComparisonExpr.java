package se.liu.albhe576.project;

public class ComparisonExpr extends Expr {
    public Expr left;
    public Expr right;
    public Token op;
    public ComparisonExpr(Expr left, Expr right, Token op, int line, String file){
        super(line,file);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads)  throws CompileException{
        left.compile(symbolTable, quads);
        Symbol lSymbol = quads.getLastResult();
        Symbol rSymbol = quads.createSetupBinary(symbolTable, right, lSymbol);
        quads.createCmp(lSymbol, rSymbol);
        quads.addQuad(QuadOp.fromToken(op), null, null, Compiler.generateSymbol(DataType.getInt()));

    }
}
