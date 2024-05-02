package se.liu.albhe576.project;

public class Label {
    @Override
    public String toString() {
        return label + ":";
    }

    private final String label;
    public Label(String label){
        this.label = label;

    }
}
