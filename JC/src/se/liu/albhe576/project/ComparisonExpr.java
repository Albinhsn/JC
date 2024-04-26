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
    private void typeCheckComparison(DataType left, DataType right) throws CompileException{
        if(left.isArray() || right.isArray() || left.isStruct() || right.isStruct() || left.isString() || right.isString()){
            Compiler.error(String.format("Can't do comparison op %s with types %s and %s", this.op.literal(), left, right), line, file);
        }
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads)  throws CompileException{
        QuadListPair quadPair = QuadList.compileBinary(symbolTable, quads, left, right);
        Symbol lResult = quads.getLastResult();

        QuadList rQuads = quadPair.right();
        Symbol rResult = rQuads.getLastResult();

        this.typeCheckComparison(lResult.type, rResult.type);
        QuadOp op = QuadOp.fromToken(this.op);
        if(lResult.type.isFloat() || rResult.type.isFloat()){
            op = convertOpToFloat(op);
        }

        DataType highestPrecedenceType = DataType.getHighestDataTypePrecedence(lResult.type, rResult.type);
        Symbol resultType = Compiler.generateSymbol(highestPrecedenceType);
        rResult = AssignStmt.convertValue(rResult, resultType, rQuads);

        lResult = quads.createSetupBinary(rQuads, lResult, rResult);
        lResult = AssignStmt.convertValue(lResult, resultType, quads);

        quads.createCmp(lResult, rResult);
        quads.addQuad(op, null, null, Compiler.generateSymbol(DataType.getInt()));
    }
}
