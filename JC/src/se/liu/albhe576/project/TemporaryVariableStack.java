package se.liu.albhe576.project;

import java.util.Stack;

public class TemporaryVariableStack extends Stack<TemporaryVariable>{
    private int maxOffset;
    private final SymbolTable symbolTable;
    private final String functionName;
    public int getMaxOffset(){
        return this.maxOffset;
    }
    public TemporaryVariableStack(SymbolTable symbolTable, String functionName){
        this.symbolTable        = symbolTable;
        this.functionName       = functionName;
    }

    public int push(DataType type){
        int offset;
        if(this.isEmpty()){
            offset = -symbolTable.getLocalVariableStackSize(functionName);
        }else{
            offset = this.peek().offset();
        }
        offset -= SymbolTable.getStructSize(symbolTable.getStructs(), type);
        this.maxOffset = Math.min(this.maxOffset, offset);
        super.push(new TemporaryVariable(offset, type));
        return offset;
    }
}
