package se.liu.albhe576.project;


public class DotExpr implements  Expr{
    @Override
    public String toString() {
        return String.format("%s%s%s", variable,accessOperation.literal, member.literal);
    }

    public Expr variable;
    public Token accessOperation;
    public Token member;

    public DotExpr(Expr variable, Token accessOperation, Token member){
        this.variable = variable;
        this.accessOperation = accessOperation;
        this.member = member;

    }
}
