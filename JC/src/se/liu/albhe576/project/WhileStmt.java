package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class WhileStmt implements  Stmt{

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(String.format("while(%s){\n", condition));
        for(Stmt stmt : body){
            s.append(String.format("\t%s\n", stmt));
        }
        return s.toString();
    }

    public Expr condition;
    public List<Stmt> body;
    public WhileStmt(Expr condition, List<Stmt> body){
        this.condition = condition;
        this.body = body;
    }

    @Override
    public List<Quad> compile(List<StructSymbol> structTable, Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        List<Quad> out  = new ArrayList<>();
        ResultSymbol condLabel = Compiler.generateLabel();
        out.add(Quad.insertLabel(condLabel));
        out.addAll(condition.compile(symbolTable));

        ResultSymbol mergeLabel = Compiler.generateLabel();
        Quad.insertJMPOnComparisonCheck(out, mergeLabel, false);

        symbolTable.push(new ArrayList<>());
        for(Stmt stmt : body){
            out.addAll(stmt.compile(structTable, symbolTable));
        }
        out.add(new Quad(QuadOp.JMP, condLabel, null, null));
        out.add(Quad.insertLabel(mergeLabel));
        symbolTable.pop();

        return out;
    }
}
