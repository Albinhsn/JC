package se.liu.albhe576.project;


import java.util.List;

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
    public Signature getSignature() throws CompileException {
        throw new CompileException("Can't get signature from this stmt");
    }

    @Override
    public BasicBlock compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        block.createRet(expr.compile(functions, block, symbols));
        return block;
    }


}
