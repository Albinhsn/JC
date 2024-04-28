package se.liu.albhe576.project;

public abstract class Address extends Operand {
    protected int offset;
    protected boolean effective;

    abstract boolean isEqual(Register register);
    abstract boolean isEqual(Label label);

}
