package se.liu.albhe576.project;

public class Instruction {
    public final String label;
    public final Operation op;
    public final Operand operand0;
    public final Operand operand1;

    public final OperationSize operationSize;


    public String emit(){
        if(label != null){
            return label + ":";
        }
        StringBuilder s = new StringBuilder();
        s.append(op.name().toLowerCase());
        if(operationSize != null){
           s.append(" ").append(operationSize.name());
        }
        if(operand0 != null){
            s.append(" ").append(operand0);
        }
        if(operand1 != null){
            s.append(", ").append(operand1);
        }
        return s.toString();
    }

    public Instruction(Operation op, Operand operand0, Operand operand1, String label, OperationSize size){
        this.op = op;
        this.operand0 = operand0;
        this.operand1 = operand1;
        this.label = label;
        this.operationSize = size;
    }
    public Instruction(Operation op, OperationSize size, Operand operand0, Immediate immediate) {
        this(op, operand0, immediate, null, size);
    }
    public Instruction(Operation op, Operand operand0, Operand operand1){

        this(op, operand0, operand1, null, null);
    }
    public Instruction(Operation op, RegisterType target, RegisterType value){
        this(op, new Register(target),new Register(value), null, null);
    }
    public Instruction(Operation op, RegisterType target, Operand operand1){
        this(op, new Register(target), operand1, null, null);
    }
    public Instruction(Operation op, RegisterType target){this(op, new Register(target), null, null, null);}
    public Instruction(Operation op, Operand operand){this(op, operand, null, null, null);}
    public Instruction(Operation op){this(op, null, null, null, null);}
    public Instruction(String label){this(null, null, null, label, null);}
    public Instruction(Operation op, Operand operand0, RegisterType value){this(op, operand0, new Register(value));}


}
