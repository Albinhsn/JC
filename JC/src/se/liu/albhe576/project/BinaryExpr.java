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

        // ToDo hoist
        left.compile(symbolTable, quads);
        Symbol lResult = quads.getLastResult();
        DataType lType = lResult.getType();

        QuadList r = new QuadList();
        right.compile(symbolTable, r);
        Symbol rResult = r.getLastResult();
        DataType rType = rResult.getType();

        if(isInvalidArithmetic(lType, rType)){
            this.error(String.format("Can't do arithmetic op '%s' on %s and %s", op.literal(), lType, rType));
        }

        DataType resultType = lType;
        QuadOp quadOp = QuadOp.fromToken(op);

        if(lType.isFloatingPoint() || rType.isFloatingPoint()){
            quadOp = quadOp.convertToFloat();
            resultType = DataType.getFloat();
        }

        // (pointer op int) or opposite
        // ToDo hoist
        if((lType.isPointer() && rType.isInteger()) || (lType.isInteger() && rType.isPointer())){
            resultType = lType.isPointer() ? lType : rType;
            QuadList quadsToIMUL = lType.isPointer() ? r : quads;

            int structSize = symbolTable.getStructSize(resultType);
            quadsToIMUL.createIMUL(String.valueOf(structSize));

        // (float op int) or opposite
        // ToDo hoist
        }else if((lType.isFloatingPoint() && rType.isInteger()) || (lType.isInteger() && rType.isFloatingPoint())){
            if(lType.isFloatingPoint()){
                rResult = r.createConvertIntToFloat(rResult);
            }else{
                lResult = quads.createConvertIntToFloat(lResult);
            }
        }

        quads.createSetupBinary(r, lResult, rResult);
        quads.addQuad(quadOp, lResult, rResult, Compiler.generateSymbol(resultType));
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
