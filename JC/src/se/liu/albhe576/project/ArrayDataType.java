package se.liu.albhe576.project;

public class ArrayDataType extends DataType{
    public final DataType itemType;
    public ArrayDataType(String name, DataTypes type, DataType itemType, int depth){
        super(name, type, depth);
        this.itemType = itemType;
    }

    public static ArrayDataType fromItemType(DataType type){
        return new ArrayDataType(type.name, DataTypes.ARRAY, type, 0);
    }

    @Override public DataType getTypeFromPointer() {
        if(depth == 0){
            return itemType;
        }
        return new ArrayDataType(name, DataTypes.ARRAY, itemType, depth - 1);
    }
}
