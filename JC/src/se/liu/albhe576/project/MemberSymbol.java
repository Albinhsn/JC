package se.liu.albhe576.project;

public class MemberSymbol extends Symbol{
    private final int offset;
    public int getOffset(){
        return this.offset;
    }

    public MemberSymbol(String name, DataType type, int offset) {
        super(name, type);
        this.offset = offset;
    }
}
