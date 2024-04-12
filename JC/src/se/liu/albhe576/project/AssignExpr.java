package se.liu.albhe576.project;

public class AssignExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s = %s",variable.literal, value);
    }

    private final Token variable;
    private final Expr value;
    public AssignExpr(Token variable, Expr value){
        this.variable = variable;
        this.value = value;

    }
}
