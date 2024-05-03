package se.liu.albhe576.project;

public class IntelInstruction extends Instruction{

    private final Operand op;
    private final Operand destination;
    private final Operand source;
    public IntelInstruction(Operand op, Operand dest, Operand source){
        this.op             = op;
        this.destination    = dest;
        this.source         = source;
    }
    public IntelInstruction(Operand op, Operand dest){
        this(op, dest, null);
    }
    public IntelInstruction(Operand op){
        this(op, null, null);
    }

    @Override
    String emit() {
        StringBuilder out = new StringBuilder(op.emit());
        if(destination != null){
            out.append(" ").append(destination.emit());
        }
        if(source != null){
            out.append(", ").append(source.emit());
        }
        return out.toString();
    }

    @Override
    boolean isRet() {
        return op.isRet();
    }
}
