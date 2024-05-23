package se.liu.albhe576.project.backend;


/**
 * DataType for arrays. Just has a field for its item type and overrides functions to manage pointers to arrays
 * @see DataType
 */
public class ArrayDataType extends DataType{
    private final DataType itemType;
    public DataType getItemType(){
        return itemType;
    }
    public static ArrayDataType fromItemType(DataType type){
        return new ArrayDataType(type.name, DataTypes.ARRAY, type, 0);
    }

    @Override public DataType getPointerFromType(){
        return new DataType(itemType.name, itemType.type, 1);
    }
    public ArrayDataType(String name, DataTypes type, DataType itemType, int depth){
        super(name, type, depth);
        this.itemType = itemType;
    }
}
