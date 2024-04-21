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


    public static void typecheckBinaryExpr(QuadOp op, DataType lType, DataType rType, Expr expr, String opLiteral) throws CompileException {
        if(QuadOp.isArithmeticOp(op)){
            if(BinaryExpr.isInvalidArithmetic(lType, rType)){
                expr.error(String.format("Can't do arithmetic op '%s' on %s and %s", opLiteral, lType, rType));
            }
        }else if(QuadOp.isBitwiseOp(op)){
            if(BinaryExpr.isInvalidBitwise(lType, rType)){
                expr.error(String.format("Can't do bitwise op '%s' on %s and %s", opLiteral, lType, rType));
            }
        }else{
            expr.error(String.format("Can't do augmented expression with op %s", opLiteral));
        }
    }

    public static boolean isInvalidBitwise(DataType left, DataType right){
        return !left.isInteger() || !right.isInteger();
    }
    public static boolean isInvalidArithmetic(DataType left, DataType right){
        if((left.isPointer() && !right.isInteger()) || (right.isPointer() && !left.isInteger())){
            return true;
        }
        return left.isArray() || left.isStruct() || right.isArray() || right.isStruct();
    }

    private void bitwise(SymbolTable symbolTable, QuadList quads) throws  CompileException {

        // ToDo hoist
        left.compile(symbolTable, quads);
        Symbol lResult = quads.getLastResult();
        quads.createPush(lResult);


        right.compile(symbolTable, quads);
        Symbol rResult = quads.getLastResult();
        quads.createMovRegisterAToC(rResult);
        quads.createPop(Compiler.generateSymbol(lResult.getType()));

        if (isInvalidBitwise(lResult.getType(), rResult.getType())) {
            this.error(String.format("Can't do bitwise with %s and %s", lResult.getType().name, rResult.getType().name));
        }

        quads.addQuad(QuadOp.fromToken(op), lResult, rResult, Compiler.generateSymbol(DataType.getInt()));
    }



    private void arithmetic(SymbolTable symbolTable, QuadList quads)  throws CompileException{

        QuadListPair quadPair = QuadList.compileBinary(symbolTable, quads, left, right);

        Symbol lResult = quads.getLastResult();
        DataType lType = lResult.getType();
        DataType resultType = lType;

        QuadList r = quadPair.right();
        Symbol rResult = r.getLastResult();
        DataType rType = rResult.getType();

        if(isInvalidArithmetic(lType, rType)){
            this.error(String.format("Can't do arithmetic op '%s' on %s and %s", op.literal(), lType, rType));
        }


        if(QuadList.isIntegerPointerBinary(lType, rType)){
            resultType = lType.isPointer() ? lType : rType;
            QuadList quadsToIMUL = lType.isPointer() ? r : quads;

            int structSize = symbolTable.getStructSize(resultType);
            quadsToIMUL.createIMUL(String.valueOf(structSize));

        }else if(QuadList.isIntegerFloatingPointBinary(lType, rType)){
            resultType = DataType.getFloat();
            SymbolPair results = QuadList.convertBinaryToSameType(quads, r, lResult, rResult);
            lResult = results.left();
            rResult = results.right();
        }

        quads.createSetupBinary(r, lResult, rResult);
        quads.addQuad(QuadOp.fromToken(op), lResult, rResult, Compiler.generateSymbol(resultType));
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
        QuadOp op = QuadOp.fromToken(this.op);
        if(QuadOp.isBitwiseOp(op)){
            this.bitwise(symbolTable, quads);
        }else if(QuadOp.isArithmeticOp(op)){
            this.arithmetic(symbolTable, quads);
        }else{
            this.error(String.format("Can't do binary op with '%s'", this.op.literal()));
        }
    }
}
