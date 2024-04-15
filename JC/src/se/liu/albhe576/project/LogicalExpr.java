package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class LogicalExpr implements Expr{

    @Override
    public String toString() {
        return String.format("%s %s %s", left, op.literal, right);
    }

    public Expr left;
    public Expr right;
    public Token op;
    public LogicalExpr(Expr left, Expr right, Token op){
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        List<Quad> l = left.compile(symbolTable);
        List<Quad> r = right.compile(symbolTable);


        ResultSymbol shortCircuitLabel = Compiler.generateLabel();
        ResultSymbol mergeLabel = Compiler.generateLabel();
        switch(op.type){
            case TOKEN_AND_LOGICAL:{
                // check first one, jump to mergeFalse, if false
                Quad.insertJMPOnComparisonCheck(l, shortCircuitLabel, false);
                l.addAll(r);
                Quad.insertJMPOnComparisonCheck(l, shortCircuitLabel, false);
                l.add(new Quad(QuadOp.LOAD_IMM, new ImmediateSymbol(new Token(TokenType.TOKEN_INT_LITERAL, 0, "1")), null,Compiler.generateResultSymbol()));
                l.add(new Quad(QuadOp.JMP, mergeLabel, null, null));
                l.add(Quad.insertLabel(shortCircuitLabel));
                l.add(new Quad(QuadOp.LOAD_IMM, new ImmediateSymbol(new Token(TokenType.TOKEN_INT_LITERAL, 0, "0")), null,Compiler.generateResultSymbol()));
                l.add(Quad.insertLabel(mergeLabel));
                break;
            }
            case TOKEN_OR_LOGICAL:{
                // check first one, jump to mergeTrue, if true
                // second one jumps over mergeTrue to merge if false
                    // remember to set it to 0 :)
                Quad.insertJMPOnComparisonCheck(l, shortCircuitLabel, true);
                l.addAll(r);
                Quad.insertJMPOnComparisonCheck(l, shortCircuitLabel, true);
                l.add(new Quad(QuadOp.LOAD_IMM, new ImmediateSymbol(new Token(TokenType.TOKEN_INT_LITERAL, 0, "0")), null,Compiler.generateResultSymbol()));
                l.add(new Quad(QuadOp.JMP, mergeLabel, null, null));
                l.add(Quad.insertLabel(shortCircuitLabel));
                l.add(new Quad(QuadOp.LOAD_IMM, new ImmediateSymbol(new Token(TokenType.TOKEN_INT_LITERAL, 0, "1")), null,Compiler.generateResultSymbol()));
                l.add(Quad.insertLabel(mergeLabel));
                break;
            }
            default: {
                Symbol lSymbol = Quad.getLastResult(l);
                Symbol rSymbol = Quad.getLastResult(r);
                l.add(new Quad(QuadOp.fromToken(op), lSymbol, rSymbol, Compiler.generateResultSymbol()));
            }
        }
        return l;
    }
}
