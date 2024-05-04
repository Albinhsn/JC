package se.liu.albhe576.project;

public class Immediate<T> extends Operand{
    private final T immediate;
    @Override
    String emit() {
        return immediate.toString();
    }
    public Immediate(T immediate){
       this.immediate = immediate;
    }
}
