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
    public List<Quad> compile(List<StructSymbol> structTable, Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        symbolTable.push(new ArrayList<>());

        List<Quad> quads = new ArrayList<>(init.compile(structTable, symbolTable));

        ResultSymbol conditionLabel = Compiler.generateLabel();
        ResultSymbol mergeLabel = Compiler.generateLabel();

        // Compile condition
        quads.add(Quad.insertLabel(conditionLabel));
        quads.addAll(condition.compile(structTable, symbolTable));

        // check if we jump
        Quad.insertJMPOnComparisonCheck(quads, mergeLabel, true);
        // Compile body
        for(Stmt stmt : body){
            quads.addAll(stmt.compile(structTable, symbolTable));
        }

        // Compile update and jumps
        quads.addAll(update.compile(structTable, symbolTable));
        quads.add(new Quad(QuadOp.JMP, conditionLabel,null, null));
        quads.add(Quad.insertLabel(mergeLabel));


        symbolTable.pop();
        return quads;
    }
}
