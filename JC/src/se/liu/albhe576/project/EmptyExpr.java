package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class EmptyExpr extends Expr{

    public EmptyExpr(int line){
        super(line);
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws CompileException {
        throw new CompileException("How can you compile an empty expression?");
    }
}
