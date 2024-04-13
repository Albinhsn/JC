package se.liu.albhe576.project;

public class Instruction {

    public InstructionType type;
    public Value operand1;
    public Value target;
    public Value operand2;
    public String emit(){
        if(this.target == null){
            if(this.operand1 != null){
                return type.name() + " " + this.operand1.location;
            }
            return type.name();
        }
        else if(this.operand1 == null){
            return type.name();
        }else if(this.operand2 == null){
            return String.format("%s %d %d\n", type.name(), target.location, operand1.location);
        }
        return String.format("%s %d = %d, %d\n", type.name(), target.location, operand1.location, operand2.location);
    }
    public Instruction(InstructionType type){
        this.type = type;
        this.target = null;
        this.operand1 = null;
        this.operand2 = null;
    }
    public Instruction(InstructionType type, Value target, Value op1, Value op2){
        this.type = type;
        this.target = target;
        this.operand1 = op1;
        this.operand2 = op2;
    }
    public Instruction(InstructionType type, Value op1){
        this.type = type;
        this.target = null;
        this.operand1 = op1;
        this.operand2 = null;
    }
    public Instruction(InstructionType type,Value target, Value op1){
        this.type = type;
        this.target = target;
        this.operand1 = op1;
        this.operand2 = null;
    }

}
