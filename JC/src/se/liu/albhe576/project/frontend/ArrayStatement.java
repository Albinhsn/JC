package se.liu.albhe576.project.frontend;

import java.util.List;

import se.liu.albhe576.project.backend.ArrayDataType;
import se.liu.albhe576.project.backend.ArrayItemSymbol;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;
import se.liu.albhe576.project.backend.DataType;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.SymbolTable;
import se.liu.albhe576.project.backend.VariableSymbol;

/**
 * Statement for array declarations
 * Includes both declaration of the variable and the contents in the array
 * @see Statement
 * @see ArrayDataType
 * @see ArrayItemSymbol
 */
public class ArrayStatement extends Statement
{
    private final ArrayDataType type;
    private final String name;
    private final List<Expression> items;
    private final int size;

    public ArrayStatement(ArrayDataType type, String name, List<Expression> items, int size, int line, String file){
        super(line, file);
        this.type = type;
        this.name = name;
        this.items = items;
        this.size = size;
    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException {
        DataType itemType   = type.getItemType();
        int itemSize        = symbolTable.getStructureSize(itemType);
        int arraySize       = itemSize * this.size;

        // the stack offset from rbp
        int stackOffset = -1 * (arraySize + symbolTable.getLocalVariableStackSize());

        // Create a variable for the array
        VariableSymbol arraySymbol = new VariableSymbol(name, DataType.getArray(itemType), stackOffset);
        symbolTable.addVariable(arraySymbol);

        // Compile every item and emit a STORE_ARRAY_ITEM instruction
        // ArrayItemSymbol contains the data needed to place the item on the stack
        for(int itemIndex = 0; itemIndex < this.items.size(); itemIndex++){
            this.items.get(itemIndex).compile(symbolTable, intermediates);
            Symbol result   = intermediates.getLastResult();
            DataType resultType = result.getType();

            // Typecheck and panic if type mismatch, convert if needed
            if(!itemType.isSameType(resultType) && !resultType.canBeConvertedTo(itemType)){
                Compiler.panic(String.format("Can't have different type then declared in array declaration, expected %s got %s", itemType, resultType), line, file);
            }else if(!itemType.isSameType(resultType)){
                intermediates.createConvert(symbolTable, result, itemType);
            }

            int offset                  = arraySymbol.getOffset() + itemIndex * itemSize;
            ArrayItemSymbol itemSymbol  = new ArrayItemSymbol(arraySymbol.getName(), arraySymbol.getType(), offset);
            intermediates.createStoreArray(itemSymbol);
        }
    }
}
