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

        QuadListPair quadPair = QuadList.compileBinary(symbolTable, quads, value, index);
        Symbol valResult = quads.getLastResult();

        if(isInvalidValueToIndex(valResult.type)){
            this.error(String.format("Can't index type %s", valResult.type));
        }

        quads.createPush(valResult);
        quads.addAll(quadPair.right());

        Symbol idxResult = quads.getLastResult();
        if(!idxResult.type.isInteger()){
            this.error(String.format("Can't index with none integer, is type %s", valResult.type));
        }

        int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), valResult.type.getTypeFromPointer());
        quads.createIMUL(String.valueOf(structSize));

        Symbol right = quads.createMovRegisterAToC(Compiler.generateSymbol(DataType.getInt()));
        Symbol left = quads.createPop(Compiler.generateSymbol(DataType.getInt()));

        Symbol addResult = quads.createAdd(left, right);
        quads.createIndex(addResult, valResult);
    }
    public IndexExpr(Expr value, Expr index, int line, String file){
        super(line, file);
        this.value = value;
        this.index = index;

    }
}
