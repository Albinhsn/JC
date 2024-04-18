package se.liu.albhe576.project;

public abstract class Stmt {

    protected final int line;
    abstract void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException;
    public Stmt(int line){
       this.line = line;
    }
}
