package se.liu.albhe576.project;

public class Label extends Address{

    @Override
    public String toString() {
        if(effective){
            return String.format("[%s]", label).toLowerCase();
        }
        return label;
    }
    private final String label;
    public Label(String label){
        this.effective = false;
        this.label = label;
    }
    public Label(String label, boolean effective){
        this.effective = effective;
        this.label = label;
    }

}
