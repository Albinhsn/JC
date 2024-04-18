package se.liu.albhe576.project;

public class EmptyExpr extends Expr{

    public EmptyExpr(int line){
        super(line);
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        throw new CompileException("How can you compile an empty expression?");
    }
}
