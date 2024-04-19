package se.liu.albhe576.project;

public class LogicalExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s %s %s", left, op.literal, right);
    }

    public Expr left;
    public Expr right;
    public Token op;
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

        boolean logical = op.type == TokenType.TOKEN_AND_LOGICAL;
        boolean jumpIfTrue = !logical;
        String firstImmediate  = logical ? "0" : "1";
        String secondImmediate = logical ? "1" : "0";


        quads.insertJMPOnComparisonCheck(shortCircuitLabel, jumpIfTrue);
        right.compile(symbolTable, quads);
        quads.insertJMPOnComparisonCheck(shortCircuitLabel, jumpIfTrue);
        quads.createLoadImmediate(DataType.getInt(), firstImmediate);
        quads.createJmp(mergeLabel);
        quads.insertLabel(shortCircuitLabel);
        quads.createLoadImmediate(DataType.getInt(), secondImmediate);
        quads.insertLabel(mergeLabel);

    }
}
