package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ForStmt implements  Stmt{
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

    public ForStmt(Stmt init, Stmt condition, Stmt update, List<Stmt> body){
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {

        List<Quad> quads = new ArrayList<>(init.compile(symbolTable));

        Symbol conditionLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();

        // Compile condition
        symbolTable.enterScope();
        quads.add(Quad.insertLabel(conditionLabel));
        quads.addAll(condition.compile(symbolTable));

        // check if we jump
        Quad.insertJMPOnComparisonCheck(quads, mergeLabel, false);
        // Compile body
        for(Stmt stmt : body){
            quads.addAll(stmt.compile(symbolTable));
        }

        // Compile update and jumps
        quads.addAll(update.compile(symbolTable));
        quads.add(new Quad(QuadOp.JMP, conditionLabel,null, null));
        quads.add(Quad.insertLabel(mergeLabel));
        symbolTable.exitScope();


        return quads;
    }
}
