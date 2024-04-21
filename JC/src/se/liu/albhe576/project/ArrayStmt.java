package se.liu.albhe576.project;

import java.util.List;

public class ArrayStmt extends Stmt {
    public final ArrayDataType type;
    public final String name;
    private final List<Expr> items;
    private final int size;

    public ArrayStmt(ArrayDataType type, String name, List<Expr> items,int size, int line, String file){
        super(line, file);
        this.type = type;
        this.name = name;
        this.items = items;
        this.size = size;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        DataType itemType = type.itemType;

        int offset = -symbolTable.getCurrentScopeSize();
        if(itemType.isStruct()){
            Struct struct = symbolTable.structs.get(itemType.name);
            offset -= struct.getSize(symbolTable.structs) * size;
        }else{
            offset -= 8 * size;
        }

        int depth = symbolTable.getDepth();
        VariableSymbol arraySymbol = new VariableSymbol(name, DataType.getArray(itemType), offset, depth);
        symbolTable.addSymbol(arraySymbol);

        for(int i = this.items.size() - 1; i >= 0; i--){
            Expr item = this.items.get(i);
            item.compile(symbolTable, quads);
            Symbol result = quads.getLastResult();
            quads.createPush(result);

            Symbol loadedImmediate = quads.createLoadImmediate(DataType.getInt(), String.valueOf(8 * i));
            quads.createMovRegisterAToC(loadedImmediate);


            Symbol offsetSymbol = Compiler.generateSymbol(DataType.getInt());
            Symbol loadedPointer = quads.createLoadPointer(arraySymbol);

            Symbol addResult = quads.createAdd(loadedPointer, offsetSymbol);
            Symbol movedArraySymbol = quads.createMovRegisterAToC(addResult);

            Symbol poppedResult = quads.createPop(result);
            quads.createStoreIndex(poppedResult, movedArraySymbol);
            if(!itemType.isSameType(result.type)){
                this.error("Can't have different types in array declaration");
            }
        }
    }
}
