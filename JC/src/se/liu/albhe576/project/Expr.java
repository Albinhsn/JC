package se.liu.albhe576.project;

public abstract class Expr {
    protected final int line;
    abstract void compile(SymbolTable symbolTable, QuadList quads) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException;
    public Expr(int line){
        this.line = line;

    }
}
