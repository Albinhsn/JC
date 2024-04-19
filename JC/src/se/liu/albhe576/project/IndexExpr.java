package se.liu.albhe576.project;

public class IndexExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s[%s]", value, index);
    }

    private final Expr value;
    private final Expr index;
    public IndexExpr(Expr value, Expr index, int line, String file){
        super(line, file);
        this.value = value;
        this.index = index;

    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException{
        // index = 1
        // value = arr
        value.compile(symbolTable, quads);
        Symbol valResult = quads.getLastResult();
        quads.createPush(valResult);

        index.compile(symbolTable, quads);
        Symbol idxResult = quads.getLastResult();

        int structSize = symbolTable.getStructSize(idxResult.type);
        quads.addQuad(QuadOp.IMUL, Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(structSize)), null, Compiler.generateSymbol(DataType.getInt()));


        Symbol right = Compiler.generateSymbol(DataType.getInt());
        quads.createMovRegisterAToC(right);


        Symbol left = Compiler.generateSymbol(DataType.getInt());
        quads.createPop(left);

        Symbol addResult = quads.createAdd(left, right);

        quads.createIndex(addResult, valResult);
    }
}
