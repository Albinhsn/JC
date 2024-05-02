package se.liu.albhe576.project;

public class Instruction <D, S>{
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if(label != null){
            return label + ":";
        }
        if(op != null){
            stringBuilder.append("\t").append(op);
        }
        if(dest != null){
            stringBuilder.append(" ").append(dest);
        }
        if(source != null){
            stringBuilder.append(", ").append(source);
        }
        return stringBuilder.toString();
    }

    private final String label;
    private final Operation op;
    private final Operand<D> dest;
    private final Operand<S> source;
    public Operation getOp(){
        return op;
    }
    public Instruction(String label){
        this.label = label;
        this.op = null;
        this.dest = null;
        this.source = null;
    }
    public Instruction(Operation op, Operand<D>operand1, Operand<S>source){
        this.label = null;
        this.op = op;
        this.dest = operand1;
        this.source = source;
    }
}
