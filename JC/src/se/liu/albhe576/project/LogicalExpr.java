package se.liu.albhe576.project;

import java.util.List;

public class LogicalExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s %s %s", left, op.literal, right);
    }

    public Expr left;
    public Expr right;
    public Token op;
    public LogicalExpr(Expr left, Expr right, Token op, int line){
        super(line);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        QuadList l = left.compile(symbolTable);
        QuadList r = right.compile(symbolTable);


        Symbol shortCircuitLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();
        switch(op.type){
            case TOKEN_AND_LOGICAL:{
                // check first one, jump to mergeFalse, if false
                Quad.insertJMPOnComparisonCheck(l, shortCircuitLabel, false);
                l.concat(r);
                Quad.insertJMPOnComparisonCheck(l, shortCircuitLabel, false);
                l.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "1"), null,Compiler.generateSymbol(DataType.getInt()));
                l.addQuad(QuadOp.JMP, mergeLabel, null, null);
                l.insertLabel(shortCircuitLabel);
                l.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "0"), null,Compiler.generateSymbol(DataType.getInt()));
                l.insertLabel(mergeLabel);
                break;
            }
            case TOKEN_OR_LOGICAL:{
                // check first one, jump to mergeTrue, if true
                // second one jumps over mergeTrue to merge if false
                    // remember to set it to 0 :)
                Quad.insertJMPOnComparisonCheck(l, shortCircuitLabel, true);
                l.concat(r);
                Quad.insertJMPOnComparisonCheck(l, shortCircuitLabel, true);
                l.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "0"), null,Compiler.generateSymbol(DataType.getInt()));
                l.addQuad(QuadOp.JMP, mergeLabel, null, null);
                l.insertLabel(shortCircuitLabel);
                l.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "1"), null,Compiler.generateSymbol(DataType.getInt()));
                l.insertLabel(mergeLabel);
                break;
            }
            default: {
                Symbol lSymbol = l.getLastResult();
                Symbol rSymbol = r.getLastResult();
                l.addQuad(QuadOp.fromToken(op), lSymbol, rSymbol, Compiler.generateSymbol(DataType.getInt()));
            }
        }
        return l;
    }
}
