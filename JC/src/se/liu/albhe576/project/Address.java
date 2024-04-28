package se.liu.albhe576.project;

public class Address {

    @Override
    public String toString() {
        if(label != null){
            return effective ? "[" + label + "]" : label;
        }
        if(effective){
            return String.format("[%s %+d]", register.name(), offset).toLowerCase();
        }
        return register.name().toLowerCase();
    }

    private final Register register;
    public int offset;
    private final String label;
    public boolean effective;

    public Address(int offset, Register register, boolean effective, String label){
        this.effective = effective;
        this.offset = offset;
        this.label = label;
        this.register = register;
    }
    public Address(Register register){
        this(0, register, false, null);
    }
    public Address(Register register, boolean effective){
        this(0, register, effective, null);
    }
    public Address(Register register, int offset){
        this(offset, register, true, null);
    }
    public Address(String label, boolean effective){
        this(0, null, effective, label);
    }
}
