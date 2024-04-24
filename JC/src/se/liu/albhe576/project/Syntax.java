package se.liu.albhe576.project;

public abstract class Syntax {
    protected final int line;
    protected final String file;
    abstract void compile(SymbolTable symbolTable, QuadList quads) throws CompileException;
    public Syntax(int line, String file){
        this.line = line;
        this.file = file;
    }
}
