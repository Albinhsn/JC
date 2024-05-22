package se.liu.albhe576.project.backend;

/**
 * Represents an immediate value in x86 (or some other ISA).
 * @param <T> the type of immediate, should only be an int or a string
 * @see Operand
 */
public class Immediate<T> extends Operand{
    private final T immediate;
    @Override
    public String emit() {
        return immediate.toString();
    }
    public Immediate(T immediate){
       this.immediate = immediate;
    }
}
