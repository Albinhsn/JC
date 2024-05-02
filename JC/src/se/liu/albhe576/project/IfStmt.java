package se.liu.albhe576.project;

import java.util.List;

public class IfStmt extends Stmt{

    private final List<IfBlock> ifBlocks;
    private final List<Stmt> elseBody;
    public IfStmt(List<IfBlock> ifBlocks, List<Stmt> elseBody, int line, String file){
        super(line, file);
        this.ifBlocks = ifBlocks;
        this.elseBody= elseBody;

    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException{

        Symbol mergeLabel = symbolTable.generateLabel();
        for(IfBlock block : this.ifBlocks){
            Symbol elseLabel = symbolTable.generateLabel();
            block.condition().compile(symbolTable, quads);
            quads.createJumpCondition(elseLabel, false);
            Stmt.compileBlock(symbolTable, quads, block.body());
            quads.createJump(mergeLabel);
            quads.insertLabel(elseLabel);
        }

        Stmt.compileBlock(symbolTable, quads, elseBody);
        quads.insertLabel(mergeLabel);
    }
}
