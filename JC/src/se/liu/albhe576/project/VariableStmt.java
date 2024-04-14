package se.liu.albhe576.project;

public class VariableStmt implements Stmt{

    @Override
    public String toString() {
        return String.format("%s %s = %s;", type, name, value);
    }

    private final StructType type;
    private final String name;
    private final Expr value;
    public VariableStmt(StructType type, String name, Expr value){
        this.type = type;
        this.name = name;
        this.value = value;
    }
}
