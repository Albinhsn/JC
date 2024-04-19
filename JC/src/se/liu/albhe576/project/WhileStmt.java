package se.liu.albhe576.project;

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
    public WhileStmt(Expr condition, List<Stmt> body, int line, String file){
        super(line, file);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        Symbol condLabel = Compiler.generateLabel();
        quads.insertLabel(condLabel);
        condition.compile(symbolTable, quads);

        Symbol mergeLabel = Compiler.generateLabel();
        Quad.insertJMPOnComparisonCheck(quads, mergeLabel, false);

        symbolTable.enterScope();
        for(Stmt stmt : body){
            stmt.compile(symbolTable, quads);
        }
        quads.addQuad(QuadOp.JMP, condLabel, null, null);
        quads.insertLabel(mergeLabel);
        symbolTable.exitScope();
    }
}
