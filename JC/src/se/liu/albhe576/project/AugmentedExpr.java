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
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException {
        target.compile(symbolTable, quads);
        Symbol targetSymbol = quads.getLastOperand1();

        value.compile(symbolTable, quads);
        Symbol valueSymbol = quads.getLastResult();

        QuadOp op = QuadOp.fromToken(this.op);
        if(targetSymbol.type.type == DataTypes.FLOAT || valueSymbol.type.type == DataTypes.FLOAT){
            op = op.convertToFloat();
        }

        quads.addQuad(op, targetSymbol, valueSymbol, targetSymbol);
        quads.addQuad(QuadOp.STORE, null, null, targetSymbol);
    }
}
