package se.liu.albhe576.project;

public class BinaryExpr extends Expr{

    private final Expr left;
    private final Expr right;
    private final Token op;

    @Override
    public String toString() {
        return String.format("(%s %s %s)", left, op.literal(), right);
    }

    public BinaryExpr(Expr left, Token op, Expr right, int line, String file){
        super(line, file);
        this.left = left;
        this.op = op;
        this.right = right;
    }


    public static boolean isInvalidBitwise(DataType left, DataType right){
        return !(left.isInteger() || left.isByte()) || !(right.isInteger() || right.isByte());
    }
    public static boolean isInvalidArithmetic(DataType left, DataType right){
        if((left.isPointer() && !right.isInteger()) || (right.isPointer() && !left.isInteger())){
            return true;
        }
        return left.isArray() || left.isStruct() || right.isArray() || right.isStruct();
    }

    private void bitwise(SymbolTable symbolTable, QuadList quads) throws  CompileException {

        QuadList rQuads = new QuadList();
        left.compile(symbolTable, quads);
        right.compile(symbolTable, rQuads);

        Symbol lResult = quads.getLastResult();
        Symbol rResult = rQuads.getLastResult();

        quads.createSetupBinary(rQuads, lResult, rResult);

        if (isInvalidBitwise(lResult.getType(), rResult.getType())) {
            Compiler.error(String.format("Can't do bitwise with %s and %s", lResult.getType().name, rResult.getType().name), line, file);
        }

        quads.addQuad(QuadOp.fromToken(op), lResult, rResult, Compiler.generateSymbol(DataType.getInt()));
    }



    private void arithmetic(SymbolTable symbolTable, QuadList quads)  throws CompileException{

        QuadListPair quadPair = QuadList.compileBinary(symbolTable, quads, left, right);

        Symbol lResult = quads.getLastResult();
        DataType lType = lResult.getType();

        QuadList r = quadPair.right();
        Symbol rResult = r.getLastResult();
        DataType rType = rResult.getType();

        if(isInvalidArithmetic(lType, rType)){
            Compiler.error(String.format("Can't do arithmetic op '%s' on %s and %s", op.literal(), lType, rType), line, file);
        }

        Symbol resultType;
        if(lType.isPointer() || rType.isPointer()){
            boolean lIsPointer = lType.isPointer();
            resultType = lIsPointer ? lResult : rResult;
            QuadList quadsToIMUL = lIsPointer ? r : quads;

            int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), resultType.type);
            quadsToIMUL.createIMUL(structSize);
        }else{
            resultType = Compiler.generateSymbol(DataType.getHighestDataTypePrecedence(lType, rType));
            lResult = AssignStmt.convertValue(lResult, resultType, quads);
            rResult = AssignStmt.convertValue(rResult, resultType, r);
        }

        quads.createSetupBinary(r, lResult, rResult);
        quads.addQuad(QuadOp.fromToken(op), lResult, rResult, resultType);
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
        QuadOp op = QuadOp.fromToken(this.op);
        if(QuadOp.isBitwiseOp(op)){
            this.bitwise(symbolTable, quads);
        }else if(QuadOp.isArithmeticOp(op)){
            this.arithmetic(symbolTable, quads);
        }else{
            Compiler.error(String.format("Can't do binary op with '%s'", this.op.literal()), line, file);
        }
    }
}
