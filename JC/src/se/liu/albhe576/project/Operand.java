package se.liu.albhe576.project;


public abstract class Operand {
    protected final boolean effective;
    protected final int offset;

    abstract OperationType getOp();


    public Operand(boolean effective, int offset){
        this.effective  = effective;
        this.offset     = offset;
    }
}
