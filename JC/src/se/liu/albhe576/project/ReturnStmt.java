package se.liu.albhe576.project;


import java.util.List;

public class ReturnStmt implements Stmt{

    @Override
    public String toString() {
        return String.format("return %s;", expr);
    }

    private final Expr expr;
    public ReturnStmt(Expr expr){
        this.expr = expr;

    }


}
