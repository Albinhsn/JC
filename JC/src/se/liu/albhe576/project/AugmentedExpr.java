package se.liu.albhe576.project;

public class AugmentedExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s %s %s", target, op.literal(), value);
    }

    private final Token op;
    private final Expr target;
    private final Expr value;
    public AugmentedExpr(Token op, Expr target, Expr value, int line, String file){
        super(line, file);
        this.op = op;
        this.target = target;
        this.value = value;

    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {

        QuadListPair quadPair = QuadList.compileBinary(symbolTable, quads, target, value);
        Symbol targetSymbol = quads.getLastOperand1();
        DataType targetType = targetSymbol.getType();


        QuadList valueQuads = quadPair.right();
        Symbol valueSymbol = valueQuads.getLastResult();
        DataType valueType = valueSymbol.getType();

        QuadOp op = QuadOp.fromToken(this.op);
        BinaryExpr.typecheckBinaryExpr(op, targetType, valueType, this, this.op.literal());

        // Augmented expressions (+= etc) have more conditions in order to be valid
        if(valueType.isPointer()){
            this.error("Can't do augmented op with a pointer on the right side");
        }else if(targetType.isPointer() && valueType.isInteger()){
            int structSize = symbolTable.getStructSize(targetType);
            valueQuads.createIMUL(String.valueOf(structSize));
        }

        valueSymbol = QuadList.convertResultToCorrectType(valueQuads, valueSymbol, targetSymbol);

        quads.createSetupBinary(valueQuads, targetSymbol, valueSymbol);
        quads.addQuad(op, targetSymbol, valueSymbol, targetSymbol);

        quads.createStore(targetSymbol);
    }
}
