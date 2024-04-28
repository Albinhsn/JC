package se.liu.albhe576.project;

public class Instruction {
    private final String label;
    private final Operation op;
    private final Operand operand0;
    private final Operand operand1;

    public String emit(){
        if(label != null){
            return label + ":";
        }
        StringBuilder s = new StringBuilder();
        s.append(op.name().toLowerCase());
        if(operand0 != null){
            s.append(" ").append(operand0);
        }
        if(operand1 != null){
            s.append(", ").append(operand1);
        }
        return s.toString();
    }

    public Instruction(Operation op, Operand operand0, Operand operand1, String label){
        this.op = op;
        this.operand0 = operand0;
        this.operand1 = operand1;
        this.label = label;
    }
    public Instruction(Operation op, Operand operand0, Operand operand1){
        this(op, operand0, operand1, null);
    }
    public Instruction(Operation op, Register target, Register value){
        this(op, new Operand(new Address(target)),new Operand(new Address(value)), null);
    }
    public Instruction(Operation op, Register target, Operand operand1){
        this(op, new Operand(new Address(target)), operand1, null);
    }
    public Instruction(Operation op, Register target){this(op, new Operand(new Address(target)), null, null);}
    public Instruction(Operation op, Operand operand){this(op, operand, null, null);}
    public Instruction(Operation op){this(op, null, null, null);}
    public Instruction(String label){this(null, null, null, label);}
    public Instruction(Operation op, Operand operand0, Register value){this(op, operand0, new Operand(new Address(value)));}

}
