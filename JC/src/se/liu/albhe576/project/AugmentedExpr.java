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

        Symbol rSymbol = quads.createSetupBinary(symbolTable, value, targetSymbol);

        QuadOp op = QuadOp.fromToken(this.op);
        if(targetSymbol.type.isFloatingPoint() || rSymbol.type.isFloatingPoint()){
            op = op.convertToFloat();
        }

        quads.addQuad(op, targetSymbol, rSymbol, targetSymbol);
        quads.createStore(targetSymbol);
    }
}
