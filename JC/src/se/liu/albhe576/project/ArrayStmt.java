package se.liu.albhe576.project;

import java.util.List;

public class ArrayStmt extends Stmt {
    private final ArrayDataType type;
    private final String name;
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

        // Calculate the offset that the variable will be at
        int offset = -symbolTable.getCurrentScopeSize();
        int itemSize;
        if(itemType.isStruct()){
            Struct struct = symbolTable.getStruct(itemType.name);
            itemSize = struct.getSize(symbolTable.getStructs());
            offset -=  itemSize * size;

        }else{
            itemSize = 8;
            offset -= itemSize * size;
        }


        // Create the symbol and add it to the symbol table
        // This will essentially allocate the space needed for the array on the stack
        VariableSymbol arraySymbol = new VariableSymbol(name, DataType.getArray(itemType), offset, symbolTable.generateVariableId());
        symbolTable.addVariable(arraySymbol);


        // Get a pointer to the top of the stack and iterate over each item
        // Compile it and push it to it's location
        for(int i = this.items.size() - 1; i >= 0; i--){
            Expr item = this.items.get(i);
            item.compile(symbolTable, quads);
            Symbol result = quads.getLastResult();

            if(!itemType.isSameType(result.type)){
                this.error("Can't have different types in array declaration");
            }

            quads.createPush(result);

            // Add the offset that the item starts at
            // Loads an immediate corresponding to item index and size of an item
            // Then adds that to the pointer which points to the top of the array

            Symbol loadedImmediate = quads.createLoadImmediate(DataType.getInt(), String.valueOf(itemSize * i));
            quads.createMovRegisterAToC(loadedImmediate);
            Symbol loadedPointer = quads.createLoadPointer(arraySymbol);
            Symbol addResult = quads.createAdd(loadedPointer, Compiler.generateSymbol(DataType.getInt()));
            Symbol movedArraySymbol = quads.createMovRegisterAToC(addResult);

            Symbol poppedResult = quads.createPop(result);
            quads.createStoreIndex(poppedResult, movedArraySymbol);
        }
    }
}
