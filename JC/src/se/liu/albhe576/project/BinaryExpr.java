package se.liu.albhe576.project;

public class BinaryExpr extends Expr{
    private final Expr left;
    private final Expr right;
    private final Token op;
    public BinaryExpr(Expr left, Token op, Expr right, int line, String file){
        super(line, file);
        this.left = left;
        this.op = op;
        this.right = right;
    }

    private boolean isInvalidBitwise(DataType left, DataType right){
        return !(left.isInteger() && right.isInteger());
    }
    private boolean isInvalidArithmetic(DataType left, DataType right){
        return false;
    }
    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
        left.compile(symbolTable, quads);
        Symbol leftResult = quads.getLastResult();
        right.compile(symbolTable, quads);
        Symbol rightResult = quads.getLastResult();

        DataType resultType = DataType.getHighestDataTypePrecedence(leftResult.type, rightResult.type);
        QuadOp op = QuadOp.fromToken(this.op.type());
        if(leftResult.type.isFloatingPoint()){
            op = op.convertToFloat();
        }

        if(op.isBitwise() && isInvalidBitwise(leftResult.type, rightResult.type)){
            Compiler.error(String.format("Can't do bitwise op with %s and %s", leftResult.type, rightResult.type), line, file);
        }else if(isInvalidArithmetic(leftResult.type, rightResult.type)){
            Compiler.error(String.format("Can't do arithmetic op with %s and %s", leftResult.type, rightResult.type), line, file);
        }

        quads.createBinaryOp(op, leftResult, rightResult, resultType);
    }
}
