package se.liu.albhe576.project;

import javax.xml.crypto.Data;
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
    public List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        List<Quad> l = left.compile(symbolTable);
        List<Quad> r = right.compile(symbolTable);


        Symbol shortCircuitLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();
        switch(op.type){
            case TOKEN_AND_LOGICAL:{
                // check first one, jump to mergeFalse, if false
                Quad.insertJMPOnComparisonCheck(l, shortCircuitLabel, false);
                l.addAll(r);
                Quad.insertJMPOnComparisonCheck(l, shortCircuitLabel, false);
                l.add(new Quad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "1"), null,Compiler.generateSymbol(DataType.getInt())));
                l.add(new Quad(QuadOp.JMP, mergeLabel, null, null));
                l.add(Quad.insertLabel(shortCircuitLabel));
                l.add(new Quad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "0"), null,Compiler.generateSymbol(DataType.getInt())));
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
                l.add(new Quad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "0"), null,Compiler.generateSymbol(DataType.getInt())));
                l.add(new Quad(QuadOp.JMP, mergeLabel, null, null));
                l.add(Quad.insertLabel(shortCircuitLabel));
                l.add(new Quad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "1"), null,Compiler.generateSymbol(DataType.getInt())));
                l.add(Quad.insertLabel(mergeLabel));
                break;
            }
            default: {
                Symbol lSymbol = Quad.getLastResult(l);
                Symbol rSymbol = Quad.getLastResult(r);
                l.add(new Quad(QuadOp.fromToken(op), lSymbol, rSymbol, Compiler.generateSymbol(DataType.getInt())));
            }
        }
        return l;
    }
}
