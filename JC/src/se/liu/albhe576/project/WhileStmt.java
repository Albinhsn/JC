package se.liu.albhe576.project;

import java.util.List;

public class WhileStmt implements  Stmt{

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(String.format("while(%s){\n", condition));
        for(Stmt stmt : body){
            s.append(String.format("\t%s\n", stmt));
        }
        return s.toString();
    }

    public Expr condition;
    public List<Stmt> body;
    public WhileStmt(Expr condition, List<Stmt> body){
        this.condition = condition;
        this.body = body;
    }

    @Override
    public Signature getSignature() throws CompileException {
        throw new CompileException("Can't get signature from this stmt");
    }

    @Override
    public BasicBlock compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        BasicBlock conditionBlock = new BasicBlock();
        BasicBlock bodyBlock = new BasicBlock();
        BasicBlock mergeBlock = new BasicBlock();
        block.next = conditionBlock;
        Value val = condition.compile(functions, conditionBlock, symbols);
        conditionBlock.createConditionalBranch(val, bodyBlock, mergeBlock);
        conditionBlock.next = mergeBlock;
        for(Stmt stmt : body){
            // ToDo add scope?
            stmt.compile(functions, bodyBlock, symbols);
        }
        bodyBlock.createUnconditionalBranch(conditionBlock);

        return mergeBlock;
    }
}
