package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ArrayExpr implements Expr{

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[");
        for(int i = 0; i < items.size(); i++){
            s.append(items.get(i));
            if(i < items.size() - 1){
                s.append(", ");
            }
        }
        s.append("]");
        return s.toString();
    }

    private final List<Expr> items;

    public ArrayExpr(List<Expr> items){
        this.items = items;
    }

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) {
        return null;
    }
}
