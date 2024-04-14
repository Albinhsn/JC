package se.liu.albhe576.project;

import java.util.List;

public class ForStmt implements  Stmt{
    @Override
    public String toString() {
        StringBuilder s= new StringBuilder(String.format("for(%s %s %s){\n", init, condition, update));
        for(Stmt stmt : body){
            s.append(String.format("\t%s\n", stmt));
        }
        s.append("}\n");
        return s.toString();
    }

    private final Stmt init;
    private final Stmt condition;
    private final Stmt update;
    private final List<Stmt> body;

    public ForStmt(Stmt init, Stmt condition, Stmt update, List<Stmt> body){
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

}
