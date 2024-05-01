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

        int itemSize = symbolTable.getStructSize(itemType);
        int stackOffset = -(itemSize * this.size + symbolTable.getLocalVariableStackSize(symbolTable.getCurrentFunctionName()));

        VariableSymbol arraySymbol = new VariableSymbol(name, DataType.getArray(itemType), stackOffset);
        symbolTable.addVariable(arraySymbol);

        for(int i = 0; i < this.items.size(); i++){
            this.items.get(i).compile(symbolTable, quads);
            Symbol result   = quads.getLastResult();

            if(!itemType.isSameType(result.type) && !result.type.canBeConvertedTo(itemType)){
                Compiler.error(String.format("Can't have different type then declared in array declaration, expected %s got %s", itemType, result.type), line, file);
            }else if(!itemType.isSameType(result.type)){
                quads.createConvert(result, itemType);
            }


            quads.createStoreArray(arraySymbol, i * itemSize);
        }
    }
}
