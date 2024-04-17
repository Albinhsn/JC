package se.liu.albhe576.project;

import java.util.List;

public class GroupedExpr extends Expr{
    @Override
    public String toString() {
        return String.format("(%s)", expr);
    }

    private final Expr expr;
    public GroupedExpr(Expr expr, int line){
        super(line);
        this.expr = expr;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        return expr.compile(symbolTable);
    }
}
