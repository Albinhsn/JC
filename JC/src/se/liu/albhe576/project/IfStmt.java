package se.liu.albhe576.project;

import java.util.List;

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
    public IfStmt(Expr condition, List<Stmt> ifBody, List<Stmt> elseBody, int line, String file){
        super(line, file);
        this.condition = condition;
        this.ifBody= ifBody;
        this.elseBody= elseBody;

    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException{
        condition.compile(symbolTable, quads);

        // insert conditional check
        QuadList ifQuad = new QuadList();
        symbolTable.enterScope();
        for(Stmt stmt : ifBody){
            stmt.compile(symbolTable, ifQuad);
        }
        symbolTable.exitScope();

        // insert unconditional jump
        QuadList elseQuad = new QuadList();
        symbolTable.enterScope();
        for(Stmt stmt : elseBody){
            stmt.compile(symbolTable, elseQuad);
        }
        symbolTable.exitScope();

        Symbol elseLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();


        Symbol ifJmpLabel = elseBody.isEmpty() ? mergeLabel : elseLabel;

        QuadOp conditionOp = quads.getLastOp();
        if(conditionOp.isSet()){
            quads.removeLastQuad();
            QuadOp jmpCondition = conditionOp.getJmpFromSet().invertJmpCondition();
            quads.createJmpOnCondition(jmpCondition, ifJmpLabel);
        }else{
            quads.insertJMPOnComparisonCheck(ifJmpLabel, false);
        }

        quads.addAll(ifQuad);
        if(!elseBody.isEmpty()){
            quads.addQuad(QuadOp.JMP, mergeLabel, null, null);
            quads.insertLabel(elseLabel);
            quads.addAll(elseQuad);
        }
        quads.insertLabel(mergeLabel);
    }
}
