package se.liu.albhe576.project;

public abstract class Stmt {

    protected final int line;
    abstract QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException;
    public Stmt(int line){
       this.line = line;
    }
}
