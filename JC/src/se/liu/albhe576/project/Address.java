package se.liu.albhe576.project;

public class Address<T> {
    protected final boolean effective;
    protected final int offset;
    public final T address;
    @Override
    public String toString() {
        if(this.effective){
           if(this.offset != 0){
               return String.format("[%s %+d]", address, this.offset);
           }
           return String.format("[%s]", address);
        }
        return address.toString();
    }

    public Address(T address, boolean effective, int offset) {
        this.effective = effective;
        this.offset = offset;
        this.address = address;
    }
    public Address(T address, boolean effective) {
        this(address, effective, 0);
    }

    public Address(T address) {
        this(address, false, 0);
    }

}
