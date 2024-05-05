package se.liu.albhe576.project.backend;

/**
 * A label is just an instruction that defines the beginnning of a block
 * Is used for function declarations
 * @see Instruction
 */
public class Label implements Instruction
{
    private final String label;
    @Override public  String emit() {
        return label + ":";
    }
    public Label(String label){
        this.label = label;
    }
}
