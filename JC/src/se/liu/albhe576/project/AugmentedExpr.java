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
    public AugmentedExpr(Token op, Expr target, Expr value, int line){
        super(line);
        this.op = op;
        this.target = target;
        this.value = value;

    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException {
        QuadList targetQuads = target.compile(symbolTable);
        Symbol targetSymbol = targetQuads.getLastOperand1();

        QuadList valueQuads = value.compile(symbolTable);
        Symbol valueSymbol = valueQuads.getLastResult();


        targetQuads.concat(valueQuads);
        QuadOp op = QuadOp.fromToken(this.op);
        if(targetSymbol.type.type == DataTypes.FLOAT || valueSymbol.type.type == DataTypes.FLOAT){
            op = op.convertToFloat();
        }

        targetQuads.addQuad(op, targetSymbol, valueSymbol, targetSymbol);
        targetQuads.addQuad(QuadOp.STORE, null, null, targetSymbol);

        return targetQuads;
    }
}
