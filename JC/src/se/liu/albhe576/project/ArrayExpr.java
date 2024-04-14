package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

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
}
