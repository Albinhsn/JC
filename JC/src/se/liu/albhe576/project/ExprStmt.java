package se.liu.albhe576.project;


import java.util.List;

public class ExprStmt extends Stmt{

    @Override
    public String toString() {
        return expr.toString() + ";";
    }

    private final Expr expr;
    public ExprStmt(Expr expr, int line){
        super(line);
        this.expr = expr;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        return expr.compile(symbolTable);
    }
}
