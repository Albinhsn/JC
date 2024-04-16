package se.liu.albhe576.project;


import java.util.List;
import java.util.Stack;

public class ReturnStmt implements Stmt{

    @Override
    public String toString() {
        return String.format("return %s;", expr);
    }

    private final Expr expr;
    public ReturnStmt(Expr expr){
        this.expr = expr;

    }


    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        List<Quad> out = expr.compile(symbolTable);
        out.add(new Quad(QuadOp.RET, null, null, null));
        return out;
    }
}
