package se.liu.albhe576.project;

public class ArgumentSymbol extends Symbol{
    private final boolean external;
    private final int count;
    private final int offset;
    public boolean getExternal(){
        return this.external;
    }
    public int getCount(){
        return this.count;
    }
    public int getOffset(){
        return this.offset;
    }
    public ArgumentSymbol(String name, DataType type, int offset, int count, boolean external) {
        super(name, type);
        this.external = external;
        this.offset = offset;
        this.count = count;
    }
}
