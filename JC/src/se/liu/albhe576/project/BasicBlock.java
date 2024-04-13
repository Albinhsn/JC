package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock {

    private String label;
    public BasicBlock next;
    private final List<Instruction> instructions;
    private final List<Value> values;

    public Value createBinary(Value left, Token op, Value right){
        Value value = new Value(values.size());
        instructions.add(new Instruction(InstructionType.getOpType(op), value, left, right));

        return null;
    }
    public Value createUnary(Value left, Token op){
        return null;
    }
    public Value createPostfix(Value value, Token op){
        return null;
    }
    public Value createIndex(Value target, Value index){
        return null;
    }
    public Value createStructLoad(List<Signature> functions, List<List<Symbol>> symbols, Value target, String member){
        return target;
    }

    public Value createStore(List<List<Symbol>> symbols, Value target, Value value){
        return target;
    }
    public Value createLookup(List<List<Symbol>> symbols, Value target){
        return target;
    }
    public Value createCall(List<Signature> functions, String name, List<Value> args){
        return null;
    }
    public void createRet(Value value){
        this.instructions.add(new Instruction(InstructionType.RET, value));
    }

    public Value createImmediate(Token value){
       Value immediate = new Value(this.values.size());
       this.values.add(immediate);
       return immediate;
    }
    public void createUnconditionalBranch(BasicBlock block){

    }
    public void createConditionalBranch(Value value, BasicBlock thenBlock, BasicBlock elseBlock){

    }
    public void createLabel(String name){
        assert this.label.isEmpty();
        this.label = name;
    }
    public Value allocateArray(List<Value> items, int symbolCount){
        return null;
    }

    public String emit(){
        StringBuilder s = new StringBuilder();
        if(!this.label.isEmpty()){
            s.append(this.label + "\n");
        }
        s.append("VALUES:\n");
        for(Value value : this.values){
            s.append(value + "\n");
        }
        s.append("\nINSTRUCTIONS:\n");
        for(Instruction instruction : this.instructions){
            s.append(instruction.emit());
        }

        return s.toString();
    }

    public BasicBlock(){
        this.instructions = new ArrayList<>();
        this.values = new ArrayList<>();
    }
}
