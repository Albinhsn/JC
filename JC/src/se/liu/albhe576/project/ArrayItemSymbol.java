package se.liu.albhe576.project;

public class ArrayItemSymbol extends VariableSymbol{
    private final int itemOffset;

    public int getOffset(){
        return this.itemOffset;
    }
    public ArrayItemSymbol(String name, DataType type, int offset, int itemOffset) {
        super(name, type, offset);
        this.itemOffset = itemOffset;
    }
}
