package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {

        QuadList quads = init.compile(symbolTable);

        Symbol conditionLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();

        // Compile condition
        symbolTable.enterScope();
        quads.insertLabel(conditionLabel);
        quads.concat(condition.compile(symbolTable));

        // check if we jump
        Quad.insertJMPOnComparisonCheck(quads, mergeLabel, false);
        // Compile body
        for(Stmt stmt : body){
            quads.concat(stmt.compile(symbolTable));
        }

        // Compile update and jumps
        quads.concat(update.compile(symbolTable));
        quads.addQuad(QuadOp.JMP, conditionLabel,null, null);
        quads.insertLabel(mergeLabel);
        symbolTable.exitScope();

        return quads;
    }

    public ForStmt(Stmt init, Stmt condition, Stmt update, List<Stmt> body, int line){
        super(line);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }
}
