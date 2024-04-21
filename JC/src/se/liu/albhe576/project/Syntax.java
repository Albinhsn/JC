package se.liu.albhe576.project;

public abstract class Syntax {
    protected final int line;
    protected final String file;
    abstract void compile(SymbolTable symbolTable, QuadList quads) throws CompileException;
    protected void error(String msg) throws CompileException{
        System.out.printf("%s:%d[%s]", file,line,msg);
        System.exit(1);
    }
    public Syntax(int line, String file){
        this.line = line;
        this.file = file;
    }
}
