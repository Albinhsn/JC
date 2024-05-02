package se.liu.albhe576.project;

public class Label extends Operand{
    @Override
    public String toString() {
        return label + ":";
    }
    private final String label;
    public Label(String label){
        super(false, 0);
        this.label = label;
    }

    @Override
    OperationType getOp() {
        return null;
    }
}
