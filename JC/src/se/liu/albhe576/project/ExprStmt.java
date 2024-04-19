package se.liu.albhe576.project;


import java.util.List;

public class ExprStmt extends Stmt{

    @Override
    public String toString() {
        return expr.toString() + ";";
    }

    private final Expr expr;
    public ExprStmt(Expr expr, int line, String file){
        super(line, file);
        this.expr = expr;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        expr.compile(symbolTable, quads);
    }
}
