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
    private boolean isInvalidArithmetic(QuadOp op, DataType left, DataType right)
    {
        if(op == QuadOp.MOD && !(left.isInteger() && right.isInteger())){
            return true;
        }
        if((left.isPointer() && !right.isInt()) || (right.isPointer() && !left.isInt())){
            return true;
        }
        return left.isArray() || left.isStruct() || right.isArray() || right.isStruct();
    }
    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
        left.compile(symbolTable, quads);
        Symbol leftResult = quads.getLastResult();

        QuadList rightQuads = new QuadList();
        right.compile(symbolTable, rightQuads);
        Symbol rightResult = rightQuads.getLastResult();



        DataType resultType = DataType.getHighestDataTypePrecedence(leftResult.type, rightResult.type);
        QuadOp op = QuadOp.getBinaryOp(this.op.type(), leftResult, rightResult);
        if(op.isBitwise() && isInvalidBitwise(leftResult.type, rightResult.type)){
            Compiler.error(String.format("Can't do bitwise op with %s and %s", leftResult.type, rightResult.type), line, file);
        }else if(isInvalidArithmetic(op, leftResult.type, rightResult.type)){
            Compiler.error(String.format("Can't do arithmetic op with %s and %s", leftResult.type, rightResult.type), line, file);
        }

        if(quads.getLastQuad().op().isLoadedImmediate() && rightQuads.getLastQuad().op().isLoadedImmediate()){
            Optimizer.optimizeConstantFolding(symbolTable, quads, rightQuads, op);
            return;
        }

        leftResult = Quad.convertType(symbolTable, quads, leftResult, resultType);
        rightResult = Quad.convertType(symbolTable, rightQuads, rightResult, resultType);

        if(leftResult.type.isPointer()){
            rightQuads.createIMul(symbolTable, rightResult, symbolTable.getStructSize(leftResult.type.getTypeFromPointer()));
        }else if(rightResult.type.isPointer()) {
            quads.createIMul(symbolTable, leftResult, symbolTable.getStructSize(rightResult.type.getTypeFromPointer()));
        }

        quads.addAll(rightQuads);
        quads.createBinaryOp(symbolTable, op, leftResult, rightResult, resultType);
    }
}
