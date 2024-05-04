package se.liu.albhe576.project;

public class Address<T> extends Operand{
    private final T address;
    private final boolean effective;
    private final int offset;

    public Address(T address, boolean effective, int offset){
        this.address    = address;
        this.effective  = effective;
        this.offset     = offset;
    }
    public Address(T address, boolean effective){
        this(address, effective, 0);
    }
    public Address(T address){
        this(address, false, 0);
    }
    @Override
    String emit() {
        if(effective){
            if(offset != 0){
                return String.format("[%s %+d]", address.toString().toLowerCase(), offset);
            }
            return String.format("[%s]", address.toString().toLowerCase());
        }
        return address.toString().toLowerCase();
    }
}
