package se.liu.albhe576.project;

public class VariableStmt extends Stmt{
    private final VariableType type;
    private final String name;
    private final Expr value;
    public VariableStmt(VariableType type, String name, Expr value){
        this.type = type;
        this.name = name;
        this.value = value;
    }
}
