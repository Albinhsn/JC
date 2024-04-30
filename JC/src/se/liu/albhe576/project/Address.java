package se.liu.albhe576.project;


public class Address extends Operand{
    String name;
    Register register;
    boolean effective;
    int offset;

    public Address(String name, boolean effective){
        this.name       = name;
        this.register   = null;
        this.effective  = effective;
        this.offset     = 0;
    }

    public Address(Register register, boolean effective, int offset){
        this.register = register;
        this.effective  = effective;
        this.offset     = offset;
    }
    public Address(Register register, boolean effective){
        this(register, effective, 0);
    }
    public Address(Register register){
        this(register, false, 0);
    }
}
