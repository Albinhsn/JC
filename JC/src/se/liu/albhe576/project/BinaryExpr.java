package se.liu.albhe576.project;

public class BinaryExpr extends Expr{

    public Expr left;
    public Expr right;
    public Token op;

    @Override
    public String toString() {
        return String.format("(%s %s %s)", left, op.literal, right);
    }

    public BinaryExpr(Expr left, Token op, Expr right, int line, String file){
        super(line, file);
        this.left = left;
        this.op = op;
        this.right = right;
    }

    public static boolean isBitwiseOp(QuadOp op){
        switch(op){
            case AND:{}
            case OR: {}
            case XOR: {}
            case SHL: {}
            case SHR: {
                return true;
            }
            default:{
                return false;
            }
        }
    }

    public static void typecheckBinaryExpr(QuadOp op, DataType lType, DataType rType, Expr expr, String opLiteral) throws CompileException {
        if(BinaryExpr.isArithmeticOp(op)){
            if(!BinaryExpr.isValidArithmetic(lType, rType)){
                expr.error(String.format("Can't do arithmetic op '%s' on %s and %s", opLiteral, lType, rType));
            }
        }else if(BinaryExpr.isBitwiseOp(op)){
            if(!BinaryExpr.isValidBitwise(lType, rType)){
                expr.error(String.format("Can't do bitwise op '%s' on %s and %s", opLiteral, lType, rType));
            }
        }else{
            expr.error(String.format("Can't do augmented expression with op %s", opLiteral));
        }
    }

    public static boolean isValidBitwise(DataType left, DataType right){
        return left.isInteger() && right.isInteger();
    }
    public static boolean isValidArithmetic(DataType left, DataType right){
        if((left.isPointer() && !right.isInteger()) || (right.isPointer() && !left.isInteger())){
            return false;
        }
        return !left.isArray() && !left.isStruct() && !right.isArray() && !right.isStruct();
    }
    public static boolean isArithmeticOp(QuadOp op){
        switch(op){
            case ADD: {}
            case SUB: {}
            case DIV: {}
            case MOD: {}
            case MUL: {
                return true;
            }
            default:{
                return false;
            }
        }
    }

    private void bitwise(SymbolTable symbolTable, QuadList quads) throws  CompileException {

        left.compile(symbolTable, quads);
        Symbol lResult = quads.getLastResult();
        quads.createPush(lResult);
        right.compile(symbolTable, quads);
        Symbol rResult = quads.getLastResult();
        quads.createMovRegisterAToC(rResult);
        quads.createPop(Compiler.generateSymbol(lResult.type));

        if (!isValidBitwise(lResult.type, rResult.type)) {
            this.error(String.format("Can't do bitwise with %s and %s", lResult.type.name, rResult.type.name));
        }

        quads.addQuad(QuadOp.fromToken(op), lResult, rResult, Compiler.generateSymbol(DataType.getInt()));
    }
    private void arithmetic(SymbolTable symbolTable, QuadList quads)  throws CompileException{

        left.compile(symbolTable, quads);
        Symbol lResult = quads.getLastResult();
        DataType lType = lResult.type;

        QuadList r = new QuadList();
        right.compile(symbolTable, r);
        Symbol rResult = r.getLastResult();
        DataType rType = rResult.type;

        if(!isValidArithmetic(lResult.type, rResult.type)){
            this.error(String.format("Can't do arithmetic op '%s' on %s and %s", op.literal, lType, rType));
        }




        DataType resultType = lResult.type;
        QuadOp quadOp = QuadOp.fromToken(op);

        if(lType.isFloatingPoint() || rType.isFloatingPoint()){
            quadOp = quadOp.convertToFloat();
            resultType = DataType.getFloat();
        }

        // int int, should be fine
        // float, float, should be fine

        // pointer op int or opposite
        if((lType.isPointer() && rType.isInteger()) || (lType.isInteger() && rType.isPointer())){
            resultType = lType.isPointer() ? lResult.type : rResult.type;
            QuadList quadsToIMUL = lType.isPointer() ? r : quads;

            int structSize = symbolTable.getStructSize(resultType);
            quadsToIMUL.createIMUL(String.valueOf(structSize));

        // float op int or opposite
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
        if(isBitwiseOp(op)){
            this.bitwise(symbolTable, quads);
        }else if(isArithmeticOp(op)){
            this.arithmetic(symbolTable, quads);
        }else{
            this.error(String.format("Can't do binary op with '%s'", this.op.literal));
        }
    }
}
