package se.liu.albhe576.project.backend;

import java.util.ArrayDeque;

/**
 * Is a stack who also keeps track of the maximum offset from the base pointer a variable is located
 * <p>
 * Is used since just using push and pop instructions doesn't solve the alignment issues you'll have when compiling for
 * something like 64 bit linux where every C function needs to be aligned to 16 bytes.
 * <p>
 * So even if you align it to 16 bytes at the start of the function, something like "a + foo(5)" would result in
 * "a" getting loaded and stores as a temporary variable and thus (if not 16 bytes) misalign the stack before calling foo which itself might call something like printf and you'll segfault
 * <p>
 * @see TemporaryVariable
 */
public class TemporaryVariableStack {
    private int maxOffset;
    private final SymbolTable symbolTable;
    private final String functionName;
    private final ArrayDeque<TemporaryVariable> temporaryStack;
    public int getMaxOffset(){
        return this.maxOffset;
    }
    public TemporaryVariableStack(SymbolTable symbolTable, String functionName){
        this.symbolTable        = symbolTable;
        this.functionName       = functionName;
        this.temporaryStack = new ArrayDeque<>();
    }
    public TemporaryVariable pop(){
        return this.temporaryStack.pop();
    }
    public TemporaryVariable peek(){
        return this.temporaryStack.peek();
    }

    public int push(DataType type){
        int offset;
        if(this.temporaryStack.isEmpty()){
            offset = -symbolTable.getLocalVariableStackSize(functionName);
        }else{
            offset = this.temporaryStack.peek().offset();
        }
        offset -= SymbolTable.getStructureSize(symbolTable.getStructures(), type);
        this.maxOffset = Math.min(this.maxOffset, offset);
        this.temporaryStack.push(new TemporaryVariable(offset, type));
        return offset;
    }
}
