package se.liu.albhe576.project;

public class Immediate extends Operand {

    @Override
    public String toString() {
        if(effective){
            return "[" + immediate + "]";
        }
        return immediate;
    }

    private final String immediate;
    public Immediate(String immediate, boolean effective) {
        super(effective, 0);
        this.immediate = immediate;

    }
    public Immediate(int immediate) {
        super(false, 0);
        this.immediate = String.valueOf(immediate);
    }
    public Immediate(String immediate) {
        super(false, 0);
        this.immediate = immediate;
    }

    @Override
    OperationType getOp() {
        return null;
    }
}
