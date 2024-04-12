package se.liu.albhe576.project;

import java.util.List;

public class ForStmt extends Stmt{
    @Override
    public String toString() {
        StringBuilder s= new StringBuilder(String.format("for(%s %s %s){\n", init, condition, update));
        for(Stmt stmt : body){
            s.append(String.format("\t%s\n", stmt));
        }
        s.append("}\n");
        return s.toString();
    }

    Stmt init;
    Stmt condition;
    Stmt update;
    List<Stmt> body;

    public ForStmt(Stmt init, Stmt condition, Stmt update, List<Stmt> body){
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }
}
