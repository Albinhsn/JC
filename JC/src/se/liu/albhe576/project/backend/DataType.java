package se.liu.albhe576.project.backend;


import se.liu.albhe576.project.frontend.*;
import java.util.Map;

/**
 * Defines functions to work with the internal structures of the language
 * A pointer is just the same data type but with a depth > 0
 * The internal data types are defines in DataTypes
 * @see DataTypes
 */
public class DataType {
    @Override
    public String toString() {return String.format("%s %s%s", type, name, depth > 0 ? "*" : "");}
    protected final String name;
    protected final DataTypes type;
    protected final int depth;
    public int getDepth(){
        return this.depth;
    }
    public DataTypes getType(){
        return this.type;
    }
    public String getName(){
        return this.name;
    }
    public boolean isFloat(){return this.type == DataTypes.FLOAT && this.depth == 0;}
    public boolean isInt(){return this.type == DataTypes.INT && this.depth == 0;}
    public boolean isShort(){return this.type == DataTypes.SHORT && this.depth == 0;}
    public boolean isLong(){return this.type == DataTypes.LONG && this.depth == 0;}
    public boolean isDouble(){return this.type == DataTypes.DOUBLE && this.depth == 0;}
    public boolean isFloatingPoint(){return isDouble()|| isFloat();}
    public boolean isStructure(){return this.type == DataTypes.STRUCT && this.depth == 0;}
    public boolean isArray(){return this.type == DataTypes.ARRAY && this.depth == 0;}
    public boolean isString(){return this.type == DataTypes.STRING && this.depth == 0;}
    public boolean isByte(){return this.type == DataTypes.BYTE && this.depth == 0;}
    public boolean isVoid(){return type == DataTypes.VOID && this.depth == 0;}
    public boolean isPointer(){return this.depth > 0;}
    private static final Map<DataTypes, Integer> DATA_TYPE_SIZE_MAP = Map.of(
            DataTypes.LONG, 8,
            DataTypes.STRING, 8,
            DataTypes.DOUBLE, 8,
            DataTypes.INT, 4,
            DataTypes.FLOAT, 4,
            DataTypes.SHORT, 2,
            DataTypes.BYTE, 1
    );
    public int getSize(){
        if(isPointer()){
	    final int pointerSize = 8;
	    return pointerSize;
        }
        return DATA_TYPE_SIZE_MAP.getOrDefault(this.type, 0);
    }
    public boolean isSameType(DataType other){
        if(other.isStructure() && this.isStructure()){
            return other.name.equals(name);
        }
        return (type == other.type && depth == 0 && other.depth == 0) || (other.depth > 0 && depth > 0);
    }
    public boolean isNumber(){return this.isInteger() || this.isFloat() || this.isDouble();}
    public boolean isInteger(){return isInt() || isByte() || isShort() || isLong();}
    public boolean canBeConvertedTo(DataType other){return (this.isNumber() && other.isNumber()) || this.isSameType(other);}

    public DataType getTypeFromPointer() {
        if(this.type == DataTypes.STRING){
            return getByte();
        }
        return new DataType(name, type, depth - 1);
    }
    public DataType getPointerFromType(){
        if(this.type == DataTypes.ARRAY){
            ArrayDataType arrayDataType = (ArrayDataType) this;
            DataType itemType = arrayDataType.getItemType();
            return new DataType(itemType.name, itemType.type, 1);
        }
        return new DataType(name, type, depth + 1);
    }
    public static DataType getInt(){return new DataType("int", DataTypes.INT, 0);}
    public static DataType getDouble(){return new DataType("double", DataTypes.DOUBLE, 0);}
    public static DataType getLong(){return new DataType("long", DataTypes.LONG, 0);}
    public static DataType getShort(){return new DataType("short", DataTypes.SHORT, 0);}
    public static DataType getArray(DataType itemType){return new ArrayDataType("array", DataTypes.ARRAY, itemType, 0);}
    public static DataType getString(){return new DataType("string", DataTypes.STRING, 0);}
    public static DataType getFloat(){return new DataType("float", DataTypes.FLOAT, 0);}
    public static DataType getStruct(String name){return new DataType(name, DataTypes.STRUCT, 0);}
    public static DataType getVoid(){return new DataType("void", DataTypes.VOID, 0);}
    public static DataType getByte(){return new DataType("byte", DataTypes.BYTE, 0);}

    public static DataType getDataTypeFromToken(Token token) throws CompileException {
        switch(token.type()){
            case TOKEN_INT, TOKEN_INT_LITERAL -> {return DataType.getInt();}
            case TOKEN_FLOAT, TOKEN_FLOAT_LITERAL -> {return DataType.getFloat();}
            case TOKEN_STRING_LITERAL, TOKEN_STRING-> {return DataType.getString();}
            case TOKEN_IDENTIFIER -> {return getStruct(token.literal());}
            case TOKEN_VOID -> {return getVoid();}
            case TOKEN_BYTE -> {return getByte();}
            case TOKEN_DOUBLE-> {return getDouble();}
            case TOKEN_SHORT-> {return getShort();}
            case TOKEN_LONG-> {return getLong();}
        }
        throw new CompileException(String.format("Can't parse value type from %s", token));
    }
    public static DataType getHighestDataTypePrecedence(DataType left, DataType right){
        if (left.isPointer() || right.isPointer()) {
            return left.isPointer() ? left : right;
        }
        else if (left.isDouble() || right.isDouble()) {
            return DataType.getDouble();
        }
        else if (left.isFloat() || right.isFloat()) {
            return DataType.getFloat();
        }
        else if (left.isLong() || right.isLong()) {
            return DataType.getLong();
        }
        else if (left.isInt() || right.isInt()) {
            return DataType.getInt();
        }
        else if (left.isShort() || right.isShort()) {
            return DataType.getShort();
        }
        return getByte();
    }
    public DataType(String name, DataTypes type, int depth){
        this.name = name;
        this.type = type;
        this.depth = depth;
    }
}
