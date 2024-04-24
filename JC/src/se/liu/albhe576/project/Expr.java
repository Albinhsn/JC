package se.liu.albhe576.project;

public class Expr extends Syntax{
    @Override
    void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
        throw new CompileException("Can't compile empty expr?");
    }
    public Expr(int line, String file){
        super(line, file);
    }

}
