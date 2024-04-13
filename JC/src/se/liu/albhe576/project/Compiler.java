package se.liu.albhe576.project;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Compiler {

    private final List<Stmt> stmts;

    private final List<Signature> functionSignatures;
    private final List<BasicBlock> blocks;

    public void Compile(String out) throws CompileException, IOException {
        for(Stmt stmt : stmts){
            functionSignatures.add(stmt.getSignature());
            System.out.println(stmt);
        }
        List<List<Symbol>> symbols = new ArrayList<>();
        for(Stmt stmt : stmts){
            symbols.add(new ArrayList<>());
            BasicBlock initialBlock = new BasicBlock();
            stmt.compile(functionSignatures, initialBlock, symbols);
            blocks.add(initialBlock);
            symbols.remove(symbols.size() - 1);
        }

        StringBuilder s = new StringBuilder();
        for(BasicBlock block : blocks){
            s.append(block.emit());
        }
        System.out.println(s);
    }

    public Compiler(List<Stmt> stmts){
        this.functionSignatures = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.stmts = stmts;
    }
}
