package se.liu.albhe576.project;

public class ComparisonExpr extends Expr {

    @Override
    public String toString() {
        return String.format("%s %s %s", left, op.literal, right);
    }

    public Expr left;
    public Expr right;
    public Token op;
    public ComparisonExpr(Expr left, Expr right, Token op, int line){
        super(line);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        left.compile(symbolTable, quads);

        Symbol lSymbol = quads.getLastResult();
        quads.addQuad(QuadOp.PUSH, null, null, Compiler.generateSymbol(lSymbol.type));
        right.compile(symbolTable, quads);
        Symbol rSymbol = quads.getLastResult();
        quads.addQuad(QuadOp.MOV_REG_CA, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));
        quads.addQuad(QuadOp.POP, null, null, Compiler.generateSymbol(lSymbol.type));
        quads.addQuad(QuadOp.CMP, lSymbol, rSymbol, null);
        quads.addQuad(QuadOp.fromToken(op), null, null, Compiler.generateSymbol(DataType.getInt()));

    }
}
