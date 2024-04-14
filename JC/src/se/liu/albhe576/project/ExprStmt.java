package se.liu.albhe576.project;


import java.util.List;

public class ExprStmt implements  Stmt{

    @Override
    public String toString() {
        return expr.toString() + ";";
    }

    private final Expr expr;
    public ExprStmt(Expr expr){
        this.expr = expr;
    }
}
