package se.liu.albhe576.project;

public class IndexExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s[%s]", value, index);
    }

    private final Expr value;
    private final Expr index;
    private static boolean isInvalidValueToIndex(DataType type){
        return !(type.isArray() || type.isPointer());
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException{
        // index = 1
        // value = arr
        value.compile(symbolTable, quads);
        Symbol valResult = quads.getLastResult();
        quads.createPush(valResult);

        if(isInvalidValueToIndex(valResult.type)){
            this.error(String.format("Can't index type %s", valResult.type));
        }

        index.compile(symbolTable, quads);
        Symbol idxResult = quads.getLastResult();

        if(!idxResult.type.isInteger()){
            this.error(String.format("Can't index with none integer, is type %s", valResult.type));
        }

        int structSize = symbolTable.getStructSize(idxResult.type);
        quads.createIMUL(String.valueOf(structSize));

        Symbol right = Compiler.generateSymbol(DataType.getInt());
        quads.createMovRegisterAToC(right);


        Symbol left = Compiler.generateSymbol(DataType.getInt());
        quads.createPop(left);

        Symbol addResult = quads.createAdd(left, right);

        quads.createIndex(addResult, valResult);
    }
    public IndexExpr(Expr value, Expr index, int line, String file){
        super(line, file);
        this.value = value;
        this.index = index;

    }
}
