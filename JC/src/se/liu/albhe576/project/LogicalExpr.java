package se.liu.albhe576.project;

public class LogicalExpr extends Expr{

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

        boolean jumpIfTrue = op.type() == TokenType.TOKEN_AND_LOGICAL;
        String fstImm = jumpIfTrue ? "1" : "0";
        String sndImm = jumpIfTrue ? "0" : "1";

        quads.createJumpOnComparison(shortCircuitLabel, jumpIfTrue);
        right.compile(symbolTable, quads);
        quads.createJumpOnComparison(shortCircuitLabel, jumpIfTrue);
        quads.createLoadImmediate(DataType.getInt(), fstImm);
        quads.addQuad(QuadOp.JMP, mergeLabel, null, null);
        quads.insertLabel(shortCircuitLabel);
        quads.createLoadImmediate(DataType.getInt(), sndImm);
        quads.insertLabel(mergeLabel);
    }
}
