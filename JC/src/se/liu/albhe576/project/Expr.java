package se.liu.albhe576.project;

public class Expr extends Syntax{
    @Override
    void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
        // Reason why we don't throw is that there exists expressions that don't have any content
        // An extra ';' is an example of this and is not indicative of anything actually gone wrong
        return;
    }
    public Expr(int line, String file){
        super(line, file);
    }

}
