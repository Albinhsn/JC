package se.liu.albhe576.project;


import java.util.List;
import java.util.Stack;

public abstract class Expr {

    protected final int line;

    abstract QuadList compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException;
    public Expr(int line){
        this.line = line;

    }
}
