package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class IfStmt implements Stmt{
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(String.format("if(%s){\n", condition));
        for(Stmt stmt: ifBody){
            s.append(String.format("\t%s\n", stmt));
        }
        s.append("}\n");

        if(elseBody != null){
            s.append("else{\n");
            for(Stmt stmt: elseBody){
                s.append(String.format("%s\n", stmt));
            }

            // java pls
            //s.append("}\n");
        }

        return s.toString();
    }

    public Expr condition;
    public List<Stmt> ifBody;
    public List<Stmt> elseBody;
    public IfStmt(Expr condition, List<Stmt> ifBody, List<Stmt> elseBody){
        this.condition = condition;
        this.ifBody= ifBody;
        this.elseBody= elseBody;

    }

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        List<Quad> out = new ArrayList<>(condition.compile(symbolTable));

        // insert conditional check
        List<Quad> ifQuad = new ArrayList<>();
        for(Stmt stmt : ifBody){
            symbolTable.push(new ArrayList<>());
            ifQuad.addAll(stmt.compile(symbolTable));
            symbolTable.pop();
        }

        // insert unconditional jump
        List<Quad> elseQuad = new ArrayList<>();
        for(Stmt stmt : elseBody){
            symbolTable.push(new ArrayList<>());
            elseQuad.addAll(stmt.compile(symbolTable));
            symbolTable.pop();
        }

        ResultSymbol elseLabel = Compiler.generateLabel();
        ResultSymbol mergeLabel = Compiler.generateLabel();

        Quad.insertJMPOnComparisonCheck(out, elseLabel, false);
        out.addAll(ifQuad);
        out.add(new Quad(QuadOp.JMP, mergeLabel, null, null));
        out.add(new Quad(QuadOp.LABEL, elseLabel, null, null));
        out.addAll(elseQuad);
        out.add(new Quad(QuadOp.LABEL, mergeLabel, null, null));

        return out;
    }
}
