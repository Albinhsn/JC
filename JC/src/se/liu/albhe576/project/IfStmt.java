package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class IfStmt implements Stmt{
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(String.format("if(%s){\n", condition));
        for(Stmt stmt: ifBody){
            s.append(String.format("\t%s\n", stmt));
        }
        s.append("}\n");

        if(elseBody != null){
            s.append("else{\n");
            for(Stmt stmt: elseBody){
                s.append(String.format("%s\n", stmt));
            }

            // java pls
            //s.append("}\n");
        }

        return s.toString();
    }

    public Expr condition;
    public List<Stmt> ifBody;
    public List<Stmt> elseBody;
    public IfStmt(Expr condition, List<Stmt> ifBody, List<Stmt> elseBody){
        this.condition = condition;
        this.ifBody= ifBody;
        this.elseBody= elseBody;

    }

    @Override
    public Signature getSignature() throws CompileException {
        throw new CompileException("Can't get signature from this stmt");
    }

    @Override
    public BasicBlock compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        Value condition = this.condition.compile(functions, block, symbols);
        BasicBlock thenBlock = new BasicBlock();
        BasicBlock elseBlock = new BasicBlock();
        BasicBlock mergeBlock = new BasicBlock();
        block.createConditionalBranch(condition, thenBlock, elseBlock);

        for(Stmt stmt : ifBody){
            thenBlock = stmt.compile(functions, thenBlock, symbols);
        }
        thenBlock.next = mergeBlock;
        for(Stmt stmt : elseBody){
            elseBlock = stmt.compile(functions, elseBlock, symbols);
        }
        elseBlock.next = mergeBlock;

        return mergeBlock;
    }
}
