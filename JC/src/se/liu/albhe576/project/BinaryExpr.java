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
    public static boolean isInvalidBitwise(DataType left, DataType right){return !(left.isLong() || left.isInt() || left.isByte() || left.isShort()) || !(right.isLong() || right.isInt() || right.isByte() || right.isShort());}
    public static boolean isInvalidArithmetic(DataType left, DataType right){
        if((left.isPointer() && !right.isInt()) || (right.isPointer() && !left.isInt())){
            return true;
        }
        return left.isArray() || left.isStruct() || right.isArray() || right.isStruct();
    }

    private void bitwise(SymbolTable symbolTable, QuadList quads) throws  CompileException {
        QuadListPair quadPair = QuadList.compileBinary(symbolTable, quads, left, right);

        Symbol lResult = quads.getLastResult();
        Symbol rResult = quadPair.right().getLastResult();

        if (isInvalidBitwise(lResult.type, rResult.type)) {
            Compiler.error(String.format("Can't do bitwise with %s and %s", lResult.type.name, rResult.type.name), line, file);
        }

        quads.createSetupBinary(quadPair.right(), lResult, rResult);
        quads.addQuad(QuadOp.fromToken(op), lResult, rResult, Compiler.generateSymbol(DataType.getInt()));
    }



    private void arithmetic(SymbolTable symbolTable, QuadList lQuads)  throws CompileException{
        QuadListPair quadPair = QuadList.compileBinary(symbolTable, lQuads, left, right);

        Symbol lResult = lQuads.getLastResult();
        DataType lType = lResult.type;

        QuadList rQuads = quadPair.right();
        Symbol rResult = rQuads.getLastResult();
        DataType rType = rResult.type;

        if(isInvalidArithmetic(lType, rType)){
            Compiler.error(String.format("Can't do arithmetic op '%s' on %s and %s", op.literal(), lType, rType), line, file);
        }

        Symbol resultType;
        if(lType.isPointer() || rType.isPointer()){
            boolean lIsPointer = lType.isPointer();
            resultType           = lIsPointer ? lResult : rResult;
            QuadList quadsToIMUL = lIsPointer ? rQuads : lQuads;

            int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), resultType.type.getTypeFromPointer());
            quadsToIMUL.createIMUL(structSize);
        }else{
            resultType = Compiler.generateSymbol(DataType.getHighestDataTypePrecedence(lType, rType));
            rResult = AssignStmt.convertValue(rResult, resultType, rQuads);
        }

        lQuads.createSetupBinary(rQuads, lResult, rResult);
        lResult = AssignStmt.convertValue(lResult, resultType, lQuads);
        lQuads.addQuad(QuadOp.fromToken(op), lResult, rResult, resultType);
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
