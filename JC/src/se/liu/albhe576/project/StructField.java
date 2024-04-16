package se.liu.albhe576.project;

public class StructField {

    @Override
    public String toString() {
        return String.format("%s %s", type, name);
    }

    public final String name;
    public final DataType type;

    public StructField(String name, DataType type, String structName) {
        this.name = name;
        this.type = type;
    }
}
