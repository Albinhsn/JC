package se.liu.albhe576.project.backend;

/**
 * Defines what an x86 instruction is
 * Which is (since label is it's own instruction) just an operation with up to two optional operands
 * There is some extensions that might be needed, but those should be done on either operation or operand
 * For an example moving an immediate to memory i.e (mov DWORD [rbp - 4], 4) can be done in operation
 * and any of the ways to do effective addressing can be done in operand
 * @see Operand
 * @see Instruction
 * @see Operation
 */
public class IntelInstruction implements Instruction
{
    private final Operation operation;
    private final Operand destination;
    private final Operand source;
    public IntelInstruction(Operation operation, Operand dest, Operand source){
        this.operation      = operation;
        this.destination    = dest;
        this.source         = source;
    }
    public IntelInstruction(Operation operation, Operand dest){
        this(operation, dest, null);
    }
    public IntelInstruction(Operation operation){
        this(operation, null, null);
    }
    @Override
    public String emit() {
        StringBuilder out = new StringBuilder(operation.name().toLowerCase());
        if(destination != null){
            out.append(" ").append(destination.emit());
        }
        if(source != null){
            out.append(", ").append(source.emit());
        }
        return out.toString();
    }
}
