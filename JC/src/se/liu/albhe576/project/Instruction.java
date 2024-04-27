package se.liu.albhe576.project;

public class Instruction {
    String label;
    Operation op;
    Operand operand0;
    Operand operand1;

    public String emit(){
        if(label != null){
            return label + ":";
        }
        StringBuilder s = new StringBuilder();
        s.append(op);
        if(operand0 != null){
            s.append(" ").append(operand0);
        }
        if(operand1 != null){
            s.append(", ").append(operand1);
        }
        return s.toString();
    }

    public Instruction(Operation op, Operand operand0, Operand operand1){
        this.op = op;
        this.operand0 = operand0;
        this.operand1 = operand1;
    }
    public Instruction(Operation op, Register target, Register value){
        this.op = op;
        this.operand0 = new Operand(new Address(target));
        this.operand1 = new Operand(new Address(value));
    }
    public Instruction(Operation op, Register target, Operand operand1){
        this.op = op;
        this.operand0 = new Operand(new Address(target));
        this.operand1 = operand1;
    }
    public Instruction(Operation op, Register target){
        this.op = op;
        this.operand0 = new Operand(new Address(target));
        this.operand1 = null;
    }
    public Instruction(Operation op, Operand operand){
        this.op = op;
        this.operand0 = operand;
        this.operand1 = null;
    }
    public Instruction(Operation op){
        this.op = op;
        this.operand0 = null;
        this.operand1 = null;
    }
    public Instruction(String label){
        this.label = label;
    }
    public Instruction(Operation op, Operand operand0, Register value){
        this.op = op;
        this.operand0 = operand0;
        this.operand1 = new Operand(new Address(value));
    }

}
