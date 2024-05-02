package se.liu.albhe576.project;


public class Operand<T> {
    @Override
    public String toString() {
        if(effective){
            String out = "[" + operand.toString();
            if(offset != 0){
                out += String.format(" %+d", offset);
            }
            out += "]";
            return out.toLowerCase();
        }
        // ToDo hoist?
        if(this.operand == null){
            return "";
        }
        return operand.toString();
    }
    private final T operand;
    private final boolean effective;
    private final int offset;
    public T getOperand(){
        return this.operand;
    }

    public Operand(T operand, boolean effective, int offset){
        this.effective  = effective;
        this.offset     = offset;
        this.operand = operand;
    }

    public Operand(T operand, boolean effective){this(operand, effective, 0);}
    public Operand(T operand){this(operand, false, 0);}
}
