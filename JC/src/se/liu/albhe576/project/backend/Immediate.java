package se.liu.albhe576.project.backend;

/**
 * This could (or should?) just be inherited by a IntImmediate and a StringImmediate rather then a generic one
 * @param <T> the type of immediate, should only be an int or a string
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
