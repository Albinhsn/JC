package se.liu.albhe576.project;

public class IndexExpr extends Expr{
    private final Expr value;
    private final Expr index;
    private static boolean isInvalidValueToIndex(DataType type){return !(type.isArray() || type.isPointer());}
    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException{

        value.compile(symbolTable, quads);
        Symbol valResult = quads.getLastResult();

        index.compile(symbolTable, quads);
        Symbol indexResult = quads.getLastResult();

        if(isInvalidValueToIndex(valResult.type)){
            Compiler.error(String.format("Can't index type %s", valResult.type), line, file);
        }
        if(!indexResult.type.isDecimal()){
            Compiler.error(String.format("Can only use integer as index not %s", indexResult.type), line, file);
        }
        if(indexResult.type.isFloatingPoint()){
            indexResult = quads.createConvert(indexResult, DataType.getLong());
        }

        quads.createIndex(valResult, indexResult);

    }
    public IndexExpr(Expr value, Expr index, int line, String file){
        super(line, file);
        this.value = value;
        this.index = index;

    }
}
