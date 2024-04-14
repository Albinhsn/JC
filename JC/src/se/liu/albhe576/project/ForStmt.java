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
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        symbolTable.push(new ArrayList<>());

        List<Quad> quads = new ArrayList<>(init.compile(symbolTable));

        // Compile condition
        int conditionPoint = quads.size();
        quads.addAll(condition.compile(symbolTable));

        // Compile body
        int bodyStart = quads.size();
        Quad condJmp = quads.get(bodyStart - 1);
        for(Stmt stmt : body){
            quads.addAll(stmt.compile(symbolTable));
        }

        // Compile update and jumps
        quads.addAll(update.compile(symbolTable));
        int jmpSize = quads.size() - conditionPoint;
        ImmediateSymbol jmpImmediate = new ImmediateSymbol(new Token(TokenType.TOKEN_INT_LITERAL, 0, String.valueOf(-jmpSize)));
        quads.add(new Quad(QuadOp.JMP, jmpImmediate,null, null));

        // Patch jump here which is kinda bad?
        int jumpOver = quads.size() - bodyStart + 1;
        condJmp.operand1 = new ImmediateSymbol(new Token(TokenType.TOKEN_INT_LITERAL, 0, String.valueOf(jumpOver)));

        symbolTable.pop();
        return quads;
    }
}
