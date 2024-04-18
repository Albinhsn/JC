package se.liu.albhe576.project;

public class IndexExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s[%s]", value, index);
    }

    private final Expr value;
    private final Expr index;
    public IndexExpr(Expr value, Expr index, int line){
        super(line);
        this.value = value;
        this.index = index;

    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        // index = 1
        // value = arr
        value.compile(symbolTable, quads);
        Symbol valResult = quads.getLastResult();
        Symbol valOperand = quads.getLastOperand1();
        quads.addQuad(QuadOp.PUSH, null, null, null);

        index.compile(symbolTable, quads);
        Symbol idxResult = quads.getLastResult();

        quads.addQuad(QuadOp.MOV_REG_CA, idxResult, null, Compiler.generateSymbol(idxResult.type));
        ImmediateSymbol immSymbol = Compiler.generateImmediateSymbol(DataType.getInt(), "8");
        quads.addQuad(QuadOp.LOAD_IMM, immSymbol, null, Compiler.generateSymbol(DataType.getInt()));
        quads.addQuad(QuadOp.MUL, Compiler.generateSymbol(DataType.getInt()), Compiler.generateSymbol(idxResult.type), Compiler.generateSymbol(DataType.getInt()));
        quads.addQuad(QuadOp.MOV_REG_CA, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));
        quads.addQuad(QuadOp.POP, null, null, Compiler.generateSymbol(DataType.getInt()));
        quads.addQuad(QuadOp.ADD, null, null, Compiler.generateSymbol(DataType.getInt()));
        quads.addQuad(QuadOp.INDEX, idxResult, valOperand, Compiler.generateSymbol(valResult.type.getTypeFromPointer()));

    }
}
