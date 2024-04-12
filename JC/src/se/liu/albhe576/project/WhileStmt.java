package se.liu.albhe576.project;

import java.util.List;

public class WhileStmt extends Stmt{

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(String.format("while(%s){", condition));
        for(Stmt stmt : body){
            s.append(String.format("\t%s", stmt));
        }
        return s.toString();
    }

    public Expr condition;
    public List<Stmt> body;
    public WhileStmt(Expr condition, List<Stmt> body){
        this.condition = condition;
        this.body = body;
    }
}
