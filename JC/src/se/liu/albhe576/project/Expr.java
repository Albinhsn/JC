package se.liu.albhe576.project;

public class Expr extends Syntax{
    @Override
    void compile(SymbolTable symbolTable, QuadList quads) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException {
        this.error("Can't compile a empty expr?");
    }
    public Expr(int line, String file){
        super(line, file);
    }

}
