package se.liu.albhe576.project;

import java.util.List;

public class DotExpr extends Expr{
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
    @Override public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        Value var = variable.compile(functions, block, symbols);
        // ToDo check proper access and value
        return block.createStructLoad(functions, symbols, var, member.literal);
    }
}
