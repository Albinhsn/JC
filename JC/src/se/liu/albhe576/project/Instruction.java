package se.liu.albhe576.project;

public class Instruction {
    @Override
    public String toString() {
        if(label != null){
            return label + ":";
        }
        StringBuilder stringBuilder = new StringBuilder();
        if(op != null){
            stringBuilder.append("\t").append(op.name().toLowerCase());
        }
        if(dest != null){
            stringBuilder.append(" ").append(dest);
        }
        if(source != null){
            stringBuilder.append(", ").append(source);
        }
        return stringBuilder.toString();
    }

    String label;
    Operation op;
    Operand dest;
    Operand source;
    public Instruction(String label){
        this.label = label;
        this.op = null;
        this.dest = null;
        this.source = null;
    }
    public Instruction(Operation op, Operand operand1, Operand source){
        this.op = op;
        this.dest = operand1;
        this.source = source;
        this.label = null;
    }
}
