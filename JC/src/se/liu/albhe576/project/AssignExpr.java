package se.liu.albhe576.project;

public class AssignExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s = %s",variable, value);
    }
    private final Expr variable;
    private final Expr value;
    public AssignExpr(Token variable, Expr value){
        this.variable = new VarExpr(variable);
        this.value = value;
    }

    public AssignExpr(Expr variable, Expr value){
        this.variable = variable;
        this.value = value;

    }
}
