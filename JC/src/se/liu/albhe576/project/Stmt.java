package se.liu.albhe576.project;

import java.util.List;

public abstract class Stmt extends Syntax{
    public Stmt(int line, String file){
       super(line, file);
    }

    public static void compileBlock(SymbolTable symbolTable, QuadList quads, List<Stmt> block) throws CompileException {
        symbolTable.enterScope();
        for(Stmt stmt : block){
            stmt.compile(symbolTable, quads);
        }
        symbolTable.exitScope();
    }

}
