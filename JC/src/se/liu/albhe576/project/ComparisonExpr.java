package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class ComparisonExpr extends Expr {

    @Override
    public String toString() {
        return String.format("%s %s %s", left, op.literal, right);
    }

    public Expr left;
    public Expr right;
    public Token op;
    public ComparisonExpr(Expr left, Expr right, Token op, int line){
        super(line);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        QuadList l = left.compile(symbolTable);
        QuadList r = right.compile(symbolTable);

        Symbol lSymbol = l.getLastResult();
        Symbol rSymbol = r.getLastResult();
        l.addQuad(QuadOp.PUSH, null, null, Compiler.generateSymbol(lSymbol.type));
        l.concat(r);
        l.addQuad(QuadOp.MOV_REG_CA, null, null, null);
        l.addQuad(QuadOp.POP, null, null, Compiler.generateSymbol(lSymbol.type));
        l.addQuad(QuadOp.CMP, lSymbol, rSymbol, null);
        l.addQuad(QuadOp.fromToken(op), null, null, Compiler.generateSymbol(DataType.getInt()));
        return l;
    }
}
