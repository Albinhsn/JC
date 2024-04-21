package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class AugmentedExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s %s %s", target, op.literal, value);
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

        target.compile(symbolTable, quads);
        Symbol targetSymbol = quads.getLastOperand1();

        QuadList valueQuads = new QuadList();
        value.compile(symbolTable, valueQuads);
        Symbol valueSymbol = valueQuads.getLastResult();

        DataType lType = targetSymbol.type;
        DataType rType = valueSymbol.type;

        QuadOp op = QuadOp.fromToken(this.op);
        BinaryExpr.typecheckBinaryExpr(op, lType, rType, this, this.op.literal);

        if(rType.isPointer()){
            this.error("Can't do augmented op with a pointer on the right side");
        }else if(lType.isPointer() && rType.isInteger()){
            int structSize = symbolTable.getStructSize(lType);
            valueQuads.createIMUL(String.valueOf(structSize));
        }

        quads.createSetupBinary(valueQuads, targetSymbol, valueSymbol);

        boolean fpOp = false;
        if(targetSymbol.type.isFloatingPoint() || valueSymbol.type.isFloatingPoint()){
            op = op.convertToFloat();
            fpOp = true;
        }

        quads.addQuad(op, targetSymbol, valueSymbol, targetSymbol);

        if(fpOp && !targetSymbol.type.isFloatingPoint()){
            quads.createConvertFloatToInt(valueSymbol);
        }

        quads.createStore(targetSymbol);
    }
}
