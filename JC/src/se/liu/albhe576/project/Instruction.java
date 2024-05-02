package se.liu.albhe576.project;

public class Instruction <O, D, S>{
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
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

    private final O op;
    private final Operand<D> dest;
    private final Operand<S> source;
    public O getOp(){
        return op;
    }
    public Instruction(O op){
        this.op     = op;
        this.dest   = null;
        this.source = null;
    }
    public Instruction(O op, Operand<D>operand1, Operand<S>source){
        this.op = op;
        this.dest = operand1;
        this.source = source;
    }
    public Instruction(O op, D operand1, S source){
        this.op = op;
        this.dest = new Operand<>(operand1);
        this.source = new Operand<>(source);
    }
    public Instruction(O op, D operand1){
        this.op = op;
        this.dest = new Operand<>(operand1);
        this.source = null;
    }
}
