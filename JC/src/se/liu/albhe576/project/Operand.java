package se.liu.albhe576.project;


public class Operand<T> {
    @Override
    public String toString() {
        if(effective){
            String out = "[" + address.toString();
            if(offset != 0){
                out += String.format(" %+d", offset);
            }
            out += "]";
            return out.toLowerCase();
        }
        return address.toString();
    }
    private final T address;
    private final boolean effective;
    private final int offset;
    public T getAddress(){
        return this.address;
    }

    public Operand(T address, boolean effective, int offset){
        this.effective  = effective;
        this.offset     = offset;
        this.address = address;
    }

    public Operand(T address, boolean effective){this(address, effective, 0);}
    public Operand(T address){this(address, false, 0);}
}
