package se.liu.albhe576.project;


import java.util.List;

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
    public Signature getSignature() throws CompileException {
        throw new CompileException("Can't get signature from this stmt");
    }

    @Override
    public BasicBlock compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        expr.compile(functions, block, symbols);
        return block;
    }
}
