package se.liu.albhe576.project;

public class LogicalExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s %s %s", left, op.literal(), right);
    }

    private final Expr left;
    private final Expr right;
    private final Token op;
    public LogicalExpr(Expr left, Expr right, Token op, int line, String file){
        super(line, file);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException {
        left.compile(symbolTable, quads);

        Symbol shortCircuitLabel = Compiler.generateLabel();
        Symbol mergeLabel = Compiler.generateLabel();

        boolean jumpIfTrue = op.type() == TokenType.TOKEN_OR_LOGICAL;
        String fstImm = jumpIfTrue ? "0" : "1";
        String sndImm = jumpIfTrue ? "1" : "0";

        quads.insertJMPOnComparisonCheck(shortCircuitLabel, jumpIfTrue);
        right.compile(symbolTable, quads);
        quads.insertJMPOnComparisonCheck(shortCircuitLabel, jumpIfTrue);
        quads.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), fstImm), null,Compiler.generateSymbol(DataType.getInt()));
        quads.addQuad(QuadOp.JMP, mergeLabel, null, null);
        quads.insertLabel(shortCircuitLabel);
        quads.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), sndImm), null,Compiler.generateSymbol(DataType.getInt()));
        quads.insertLabel(mergeLabel);

    }
}
