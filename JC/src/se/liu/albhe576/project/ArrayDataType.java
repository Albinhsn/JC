package se.liu.albhe576.project;

public class ArrayDataType extends DataType{
    public DataType itemType;
    public ArrayDataType(String name, DataTypes type, DataType itemType, int depth){
        super(name, type, depth);
        this.itemType = itemType;
    }

    public static ArrayDataType fromDataType(DataType type){
        return new ArrayDataType(type.name, DataTypes.ARRAY, type, 0);
    }

    @Override public DataType getTypeFromPointer() throws CompileException {
        return itemType;
    }
}
