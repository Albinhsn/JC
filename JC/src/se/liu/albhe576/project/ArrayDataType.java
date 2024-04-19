package se.liu.albhe576.project;

public class ArrayDataType extends DataType{
    public DataType itemType;
    public ArrayDataType(String name, DataTypes type, DataType itemType, int depth){
        super(name, type, depth);
        this.itemType = itemType;
    }

    @Override public DataType getTypeFromPointer() throws CompileException {
        return itemType;
    }
}
