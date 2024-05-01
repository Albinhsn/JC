package se.liu.albhe576.project;

import java.util.List;
public class ForStmt extends Stmt{
    private final Stmt init;
    private final Stmt condition;
    private final Stmt update;
    private final List<Stmt> body;
    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException{

        symbolTable.enterScope();
        init.compile(symbolTable, quads);

        Symbol conditionLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();

        // Compile condition
        quads.insertLabel(conditionLabel);
        condition.compile(symbolTable, quads);

        // check if we jump
        quads.createJumpCondition(mergeLabel, false);

        // Compile body
        for(Stmt stmt : body){
            stmt.compile(symbolTable, quads);
        }

        // Compile update and jumps
        update.compile(symbolTable, quads);
        quads.createJump(conditionLabel);
        quads.insertLabel(mergeLabel);
        symbolTable.exitScope();
    }

    public ForStmt(Stmt init, Stmt condition, Stmt update, List<Stmt> body, int line, String file){
        super(line, file);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }
}
