package se.liu.albhe576.project;

public class VariableStmt extends Stmt{

    @Override
    public String toString() {
        return String.format("%s %s = %s;", type, name, value);
    }

    private final VariableType type;
    private final String name;
    private final Expr value;
    public VariableStmt(VariableType type, String name, Expr value){
        this.type = type;
        this.name = name;
        this.value = value;
    }
}
