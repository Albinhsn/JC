package se.liu.albhe576.project;

import java.util.Map;
import java.util.Stack;

public class TemporaryVariableStack {
    private int maxOffset;
    private final SymbolTable symbolTable;
    private final String functionName;
    private final Stack<TemporaryStackVariable> temporaryStack;
    public int getMaxOffset(){
        return this.maxOffset;
    }
    public TemporaryVariableStack(SymbolTable symbolTable, String functionName){
        this.symbolTable        = symbolTable;
        this.functionName       = functionName;
        this.temporaryStack     = new Stack<>();
    }
    public TemporaryStackVariable peekVariable(){
        return this.temporaryStack.peek();
    }

    public TemporaryStackVariable popVariable(){
        return temporaryStack.pop();
    }

    public int pushVariable(DataType type){
        int offset;
        if(temporaryStack.isEmpty()){
            offset = -symbolTable.getLocalVariableStackSize(functionName);
        }else{
            offset = temporaryStack.peek().offset();
        }
        offset -= SymbolTable.getStructSize(symbolTable.getStructs(), type);
        this.maxOffset = Math.min(this.maxOffset, offset);
        temporaryStack.push(new TemporaryStackVariable(offset, type));
        return offset;
    }
}
