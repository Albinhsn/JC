package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class EmptyExpr implements Expr{

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) {
        return null;
    }
}
