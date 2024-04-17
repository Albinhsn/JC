package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class WhileStmt extends Stmt{

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
    public WhileStmt(Expr condition, List<Stmt> body, int line){
        super(line);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        QuadList out  = new QuadList();
        Symbol condLabel = Compiler.generateLabel();
        out.insertLabel(condLabel);
        out.concat(condition.compile(symbolTable));

        Symbol mergeLabel = Compiler.generateLabel();
        Quad.insertJMPOnComparisonCheck(out, mergeLabel, false);

        symbolTable.enterScope();
        for(Stmt stmt : body){
            out.concat(stmt.compile(symbolTable));
        }
        out.addQuad(QuadOp.JMP, condLabel, null, null);
        out.insertLabel(mergeLabel);
        symbolTable.exitScope();

        return out;
    }
}
