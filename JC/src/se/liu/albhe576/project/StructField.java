package se.liu.albhe576.project;

public class StructField {

    @Override
    public String toString() {
        return String.format("%s %s", type, name);
    }

    public final String name;
    public final StructType type;

    public StructField(String name, StructType type) {
        this.name = name;
        this.type = type;
    }
}
