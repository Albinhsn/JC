package se.liu.albhe576.project;

public class VariableSymbol extends Symbol{
    private StructType type;
    public VariableSymbol(StructType type, String name) {
        super(name);
        this.type = type;
    }
}
