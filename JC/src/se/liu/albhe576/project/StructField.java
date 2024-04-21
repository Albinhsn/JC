package se.liu.albhe576.project;

public record StructField(String name, DataType type) {

    @Override
    public String toString() {
        return String.format("%s %s", type, name);
    }

}
