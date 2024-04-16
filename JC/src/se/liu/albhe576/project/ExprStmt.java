package se.liu.albhe576.project;


import java.util.List;
import java.util.Stack;

public class ExprStmt implements  Stmt{

    @Override
    public String toString() {
        return expr.toString() + ";";
    }

    private final Expr expr;
    public ExprStmt(Expr expr){
        this.expr = expr;
    }

    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        return expr.compile(symbolTable);
    }
}
