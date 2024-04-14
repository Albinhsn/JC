package se.liu.albhe576.project;

import java.util.List;

public class IndexExpr implements Expr{

    @Override
    public String toString() {
        return String.format("%s[%s]", value, index);
    }

    private final Expr value;
   private final Expr index;
    public IndexExpr(Expr value, Expr index){
        this.value = value;
        this.index = index;

    }
}
