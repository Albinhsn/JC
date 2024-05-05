package se.liu.albhe576.project.backend;

/**
 * Base class for what an operand is.
 * @see Address
 * @see Immediate
 */
public abstract class Operand {
    @Override
    public String toString() {
        return emit();
    }
    public abstract String emit();
}


