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

    private static QuadOp convertOpToFloat(QuadOp op) {
        switch(op){
            case SETLE -> {return QuadOp.SETBE ;}
            case SETL -> {return QuadOp.SETB;}
            case SETG -> {return QuadOp.SETA;}
            case SETGE -> {return QuadOp.SETAE;}
            default -> {return op;}
        }
    }

    private void typecheckComparison(DataType left, DataType right) throws CompileException {
        if(left.isArray() || right.isArray() || left.isStruct() || right.isStruct() || left.isString() || right.isString()){
            this.error(String.format("Can't do comparison op %s with types %s and %s", this.op.literal(), left, right));
        }
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads)  throws CompileException{

        QuadListPair quadPair = QuadList.compileBinary(symbolTable, quads, left, right);
        Symbol lResult = quads.getLastResult();
        DataType lType = lResult.getType();

        QuadList rQuads = quadPair.right();
        Symbol rResult = rQuads.getLastResult();
        DataType rType = rResult.getType();

        this.typecheckComparison(lType, rType);

        QuadOp op = QuadOp.fromToken(this.op);
        if(lType.isFloatingPoint() || rType.isFloatingPoint()){
            op = convertOpToFloat(op);
        }

        SymbolPair result = QuadList.convertBinaryToSameType(quads, rQuads, lResult, rResult);
        lResult = result.left();
        rResult = result.right();


        quads.createSetupBinary(rQuads, lResult, rResult);
        quads.createCmp(lResult, rResult);
        quads.addQuad(op, null, null, Compiler.generateSymbol(DataType.getInt()));
    }
}
