package se.liu.albhe576.project;

public class ArgumentSymbol extends Symbol{
    boolean external;
    int count;
    int offset;
    public ArgumentSymbol(String name, DataType type, int offset, int count, boolean external) {
        super(name, type);
        this.external = external;
        this.offset = offset;
        this.count = count;
    }
}
