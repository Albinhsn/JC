package se.liu.albhe576.project;

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
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        left.compile(symbolTable, quads);

        Symbol shortCircuitLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();
        switch(op.type){
            case TOKEN_AND_LOGICAL:{
                // check first one, jump to mergeFalse, if false
                Quad.insertJMPOnComparisonCheck(quads, shortCircuitLabel, false);
                right.compile(symbolTable, quads);
                Quad.insertJMPOnComparisonCheck(quads, shortCircuitLabel, false);
                quads.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "1"), null,Compiler.generateSymbol(DataType.getInt()));
                quads.addQuad(QuadOp.JMP, mergeLabel, null, null);
                quads.insertLabel(shortCircuitLabel);
                quads.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "0"), null,Compiler.generateSymbol(DataType.getInt()));
                quads.insertLabel(mergeLabel);
                break;
            }
            case TOKEN_OR_LOGICAL:{
                // check first one, jump to mergeTrue, if true
                // second one jumps over mergeTrue to merge if false
                    // remember to set it to 0 :)
                Quad.insertJMPOnComparisonCheck(quads, shortCircuitLabel, true);
                right.compile(symbolTable, quads);
                Quad.insertJMPOnComparisonCheck(quads, shortCircuitLabel, true);
                quads.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "0"), null,Compiler.generateSymbol(DataType.getInt()));
                quads.addQuad(QuadOp.JMP, mergeLabel, null, null);
                quads.insertLabel(shortCircuitLabel);
                quads.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), "1"), null,Compiler.generateSymbol(DataType.getInt()));
                quads.insertLabel(mergeLabel);
                break;
            }
            default: {
                throw new CompileException("How could this happen to me");
            }
        }
    }
}
