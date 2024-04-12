package se.liu.albhe576.project;

public class StructField {

    @Override
    public String toString() {
        return String.format("%s %s", type, name);
    }

    private final String name;
    private final VariableType type;

    public StructField(String name, VariableType type) {
        this.name = name;
        this.type = type;
    }
}
