package se.liu.albhe576.project;

import java.util.Stack;

public class TemporaryVariableStack {
    Stack<VariableSymbol> variables;
    int maxOffset;
    int initialOffset;

    public VariableSymbol peek(){
        return this.variables.peek();
    }

    public int pushVariable(String name, DataType type) throws CompileException{
        if(type.isStruct() || type.isArray()){
            throw new CompileException("Can't push a struct/array a temporary?");
        }
        int offset = -type.getSize();
        if(!variables.isEmpty()){
            offset += variables.peek().offset;
        }else{
            offset += initialOffset;
        }

        variables.push(new VariableSymbol(name, type, offset));
        this.maxOffset = Math.min(offset, this.maxOffset);
        return offset;
    }
    public VariableSymbol popVariable(){
        return variables.pop();
    }

    public TemporaryVariableStack(int initialOffset){
        this.maxOffset      = -initialOffset;
        this.initialOffset  = -initialOffset;
        this.variables      = new Stack<>();
    }
}
