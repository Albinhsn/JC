package se.liu.albhe576.project;

public class StructField {

    @Override
    public String toString() {
        return String.format("%s %s", type, name);
    }

    public final String name;
    public final SymbolType type;

    public StructField(String name, SymbolType type) {
        this.name = name;
        this.type = type;
    }
}
