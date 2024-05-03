package se.liu.albhe576.project;

public class Label extends Operand{
    private final String label;
    @Override
    String emit() {
        return label + ":";
    }
    public Label(String label){
        this.label = label;
    }
}
