package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class IfStmt extends Stmt{
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
    public IfStmt(Expr condition, List<Stmt> ifBody, List<Stmt> elseBody, int line){
        super(line);
        this.condition = condition;
        this.ifBody= ifBody;
        this.elseBody= elseBody;

    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        QuadList out = condition.compile(symbolTable);

        // insert conditional check
        QuadList ifQuad = new QuadList();
        symbolTable.enterScope();
        for(Stmt stmt : ifBody){
            ifQuad.concat(stmt.compile(symbolTable));
        }
        symbolTable.exitScope();

        // insert unconditional jump
        QuadList elseQuad = new QuadList();
        symbolTable.enterScope();
        for(Stmt stmt : elseBody){
            elseQuad.concat(stmt.compile(symbolTable));
        }
        symbolTable.exitScope();

        Symbol elseLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();

        Quad.insertJMPOnComparisonCheck(out, elseLabel, false);
        out.concat(ifQuad);
        out.addQuad(QuadOp.JMP, mergeLabel, null, null);
        out.addQuad(QuadOp.LABEL, elseLabel, null, null);
        out.concat(elseQuad);
        out.addQuad(QuadOp.LABEL, mergeLabel, null, null);

        return out;
    }
}
