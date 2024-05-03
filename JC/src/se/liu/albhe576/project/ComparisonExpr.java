package se.liu.albhe576.project;

public class ComparisonExpr extends Expr {
    private final Expr left;
    private final Expr right;
    private final Token op;
    public ComparisonExpr(Expr left, Expr right, Token op, int line, String file){
        super(line,file);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    private void typeCheckComparison(DataType left, DataType right) throws CompileException{
        boolean leftIsValid = left.isDecimal() || left.isPointer();
        boolean rightIsValid = right.isDecimal() || right.isPointer();
        if(!(leftIsValid && rightIsValid)){
            Compiler.error(String.format("Can't do comparison op %s with types %s and %s", this.op.literal(), left, right), line, file);
        }
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads)  throws CompileException{
        left.compile(symbolTable, quads);
        Symbol lResult = quads.getLastResult();

        QuadList rQuads = new QuadList();
        right.compile(symbolTable, rQuads);
        Symbol rResult = rQuads.getLastResult();

        this.typeCheckComparison(lResult.type, rResult.type);
        DataType resultType = DataType.getHighestDataTypePrecedence(lResult.type, rResult.type);

        QuadOp op = QuadOp.fromToken(this.op.type());

        lResult             = Quad.convertType(symbolTable, quads, lResult, resultType);
        rResult             = Quad.convertType(symbolTable, rQuads, rResult, resultType);

        quads.addAll(rQuads);
        quads.createComparison(symbolTable, op, lResult, rResult);
    }
}
