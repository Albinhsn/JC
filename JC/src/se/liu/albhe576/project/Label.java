package se.liu.albhe576.project;

public class Label extends Address{

    @Override
    public String toString() {
        if(effective){
            return String.format("[%s]", label).toLowerCase();
        }
        return label;
    }

    @Override
    public boolean isEffective() {
        return effective;
    }
    @Override
    public boolean isImmediate() {
        return effective;
    }

    public final String label;

    public Label(String label){
        this.effective = false;
        this.label = label;
    }
    public Label(String label, boolean effective){
        this.effective = effective;
        this.label = label;
    }

    @Override
    boolean isEqual(Register register) {
        return false;
    }

    @Override
    boolean isEqual(Label label) {
        return label.label.equals(this.label) && this.effective == label.effective;
    }
}
