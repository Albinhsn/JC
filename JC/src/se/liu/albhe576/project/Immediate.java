package se.liu.albhe576.project;

public class Immediate extends Operand{
    @Override
    public String toString() {
        return immediate;
    }

    public final String immediate;

    public Immediate(String immediate){
        this.immediate = immediate;
    }

}
