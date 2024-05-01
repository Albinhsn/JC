package se.liu.albhe576.project;

import java.util.List;

public class WhileStmt extends Stmt{
    private final Expr condition;
    private final List<Stmt> body;
    public WhileStmt(Expr condition, List<Stmt> body, int line, String file){
        super(line, file);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        Symbol condLabel = Compiler.generateLabel();
        quads.insertLabel(condLabel);
        condition.compile(symbolTable, quads);

        Symbol mergeLabel = Compiler.generateLabel();
        quads.createJumpCondition(mergeLabel, false);

        symbolTable.enterScope();
        for(Stmt stmt : body){
            stmt.compile(symbolTable, quads);
        }
        quads.createJump(condLabel);
        quads.insertLabel(mergeLabel);
        symbolTable.exitScope();
    }
}
