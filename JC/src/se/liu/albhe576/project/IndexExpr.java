package se.liu.albhe576.project;

public class IndexExpr extends Expr{
    private final Expr value;
    private final Expr index;
    private static boolean isInvalidValueToIndex(DataType type){return !(type.isArray() || type.isPointer());}
    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException{
        QuadListPair quadPair = QuadList.compileBinary(symbolTable, quads, value, index);
        Symbol valResult = quads.getLastResult();
        if(valResult.type.isArray()){
            ArrayDataType array = (ArrayDataType) valResult.type;
            valResult = Compiler.generateSymbol(DataType.getPointerFromType(array.itemType));
        }
        Symbol idxResult = quadPair.right().getLastResult();

        if(isInvalidValueToIndex(valResult.type)){
            Compiler.error(String.format("Can't index type %s", valResult.type), line, file);
        }

        idxResult = AssignStmt.convertValue(idxResult, Compiler.generateSymbol(DataType.getLong()), quadPair.right());
        if(!idxResult.type.isInteger()){
            Compiler.error(String.format("Can't use type %s as index", idxResult.type), line, file);
        }

        int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), valResult.type.getTypeFromPointer());
        quadPair.right().createIMUL(structSize);

        quads.createSetupBinary(quadPair.right(), valResult, Compiler.generateSymbol(DataType.getLong()));
        quads.createAdd(valResult, Compiler.generateSymbol(DataType.getLong()));
        quads.addQuad(QuadOp.LOAD, valResult, null, Compiler.generateSymbol(valResult.type.getTypeFromPointer()));
    }
    public IndexExpr(Expr value, Expr index, int line, String file){
        super(line, file);
        this.value = value;
        this.index = index;

    }
}
