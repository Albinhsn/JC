package se.liu.albhe576.project;

import java.util.List;

public class IfStmt extends Stmt{

    private final Expr condition;
    private final List<Stmt> ifBody;
    private final List<Stmt> elseBody;
    public IfStmt(Expr condition, List<Stmt> ifBody, List<Stmt> elseBody, int line, String file){
        super(line, file);
        this.condition = condition;
        this.ifBody= ifBody;
        this.elseBody= elseBody;

    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException{
        condition.compile(symbolTable, quads);

        QuadList ifQuad = new QuadList();
        Stmt.compileBlock(symbolTable, ifQuad, ifBody);

        QuadList elseQuad = new QuadList();
        Stmt.compileBlock(symbolTable, elseQuad, elseBody);

        Symbol elseLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();

        Symbol ifJmpLabel = elseBody.isEmpty() ? mergeLabel : elseLabel;
        quads.createJumpOnComparison(ifJmpLabel, true);

        quads.addAll(ifQuad);
        if(!elseBody.isEmpty()){
            quads.addQuad(QuadOp.JMP, mergeLabel, null, null);
            quads.insertLabel(elseLabel);
            quads.addAll(elseQuad);
        }
        quads.insertLabel(mergeLabel);
    }
}
