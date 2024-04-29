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
    public static boolean isInvalidBitwise(DataType left, DataType right){return !(left.isInteger() && right.isInteger());}
    public static boolean isInvalidArithmetic(DataType left, DataType right, QuadOp op){
        if(op == QuadOp.MOD && !(left.isInteger() && right.isInteger())){
            return true;
        }
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
            Compiler.error(String.format("Invalid bitwise operation with %s and %s", lResult.type.name, rResult.type.name), line, file);
        }

        quads.createSetupBinary(quadPair.right(), lResult, rResult);
        quads.addQuad(QuadOp.fromToken(op), lResult, rResult, Compiler.generateSymbol(DataType.getInt()));
    }

    private void arithmetic(SymbolTable symbolTable, QuadList lQuads)  throws CompileException{
        QuadListPair quadPair = QuadList.compileBinary(symbolTable, lQuads, left, right);
        QuadOp op = QuadOp.fromToken(this.op);

        Symbol lResult = lQuads.getLastResult();
        DataType lType = lResult.type;

        QuadList rQuads = quadPair.right();
        Symbol rResult = rQuads.getLastResult();
        DataType rType = rResult.type;

        if(isInvalidArithmetic(lType, rType, op)){
            Compiler.error(String.format("Invalid arithmetic operation, can't do '%s' on %s and %s", this.op.literal(), lType, rType), line, file);
        }

        Symbol resultType;
        if(lType.isPointer() || rType.isPointer()){
            boolean lIsPointer = lType.isPointer();
            resultType           = lIsPointer ? lResult : rResult;
            QuadList quadsToIMUL = lIsPointer ? rQuads : lQuads;

            int structSize = symbolTable.getStructSize(resultType.type.getTypeFromPointer());
            quadsToIMUL.createIMUL(structSize);
        }else{
            resultType = Compiler.generateSymbol(DataType.getHighestDataTypePrecedence(lType, rType));
            rResult = AssignStmt.convertValue(rResult, resultType, rQuads);
        }

        lResult = AssignStmt.convertValue(lResult, resultType, lQuads);
        lQuads.createSetupBinary(rQuads, lResult, rResult);
        lQuads.addQuad(op, lResult, rResult, resultType);
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
        QuadOp op = QuadOp.fromToken(this.op);
        if(QuadOp.isBitwiseOp(op)){
            this.bitwise(symbolTable, quads);
        }else if(QuadOp.isArithmeticOp(op)){
            this.arithmetic(symbolTable, quads);
        }else{
            throw new CompileException(String.format("Can't do binary op with '%s'", this.op.literal()));
        }
    }
}
