package se.liu.albhe576.project;

public class Operand {
    @Override
    public String toString() {
        if(address == null){
            return  immediate;
        }
        return address.toString();
    }

    Address address;
    String immediate;

    public Operand(String immediate){
        this.immediate = immediate;
        this.address = null;
    }
    public Operand(Address address){
        this.address = address;
        this.immediate = null;
    }
}
