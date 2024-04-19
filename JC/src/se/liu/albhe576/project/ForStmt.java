package se.liu.albhe576.project;

import java.util.List;
public class ForStmt extends Stmt{
    @Override
    public String toString() {
        StringBuilder s= new StringBuilder(String.format("for(%s %s %s){\n", init, condition, update));
        for(Stmt stmt : body){
            s.append(String.format("\t%s\n", stmt));
        }
        s.append("}\n");
        return s.toString();
    }

    private final Stmt init;
    private final Stmt condition;
    private final Stmt update;
    private final List<Stmt> body;


    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException{

        init.compile(symbolTable, quads);

        Symbol conditionLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();

        // Compile condition
        symbolTable.enterScope();
        quads.insertLabel(conditionLabel);
        condition.compile(symbolTable, quads);

        // check if we jump
        quads.insertJMPOnComparisonCheck(mergeLabel, false);
        // Compile body
        for(Stmt stmt : body){
            stmt.compile(symbolTable, quads);
        }

        // Compile update and jumps
        update.compile(symbolTable, quads);
        quads.addQuad(QuadOp.JMP, conditionLabel,null, null);
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
