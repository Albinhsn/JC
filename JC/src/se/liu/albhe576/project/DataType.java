package se.liu.albhe576.project;

public class DataType {
    @Override
    public String toString() {return String.format("%s %s", name, depth > 0 ? "*" : "");}
    protected final String name;
    protected final DataTypes type;
    protected final int depth;
    public boolean isFloat(){return this.type == DataTypes.FLOAT && depth == 0;}
    public boolean isInteger(){return this.type == DataTypes.INT && depth == 0;}
    public boolean isShort(){return this.type == DataTypes.SHORT && depth == 0;}
    public boolean isLong(){return this.type == DataTypes.LONG && depth == 0;}
    public boolean isDouble(){return this.type == DataTypes.DOUBLE && depth == 0;}
    public boolean isStruct(){return this.type == DataTypes.STRUCT && depth == 0;}
    public boolean isArray(){return this.type == DataTypes.ARRAY && depth == 0;}
    public boolean isString(){return this.type == DataTypes.STRING && depth == 0;}
    public boolean isByte(){return this.type == DataTypes.BYTE && depth == 0;}
    public boolean isPointer(){return this.depth > 0;}
    public int getSize(){
        if(isPointer() || isLong() || isDouble()){
            return 8;
        }
        if(isInteger() || isFloat()){
            return 4;
        }
        return isShort() ? 2 : 1;
    }
    public boolean isSameType(DataType other){
        if(other.isStruct() && this.isStruct()){
            return other.name.equals(name);
        }
        return ((type == other.type && depth == 0 && other.depth == 0)) || (depth > 0 && other.depth > 0);
    }
    private boolean isDecimal(){return this.isLong() || this.isShort() || this.isByte() || this.isInteger() || this.isFloat();}
    public boolean canBeCastedTo(DataType other){return this.isDecimal() && other.isDecimal() || this.isSameType(other);}

    public DataType getTypeFromPointer() {
        if(this.type == DataTypes.STRING && this.depth == 1){
            return getByte();
        }
        return new DataType(this.name, this.type, this.depth - 1);
    }
    public static DataType getPointerFromType(DataType type){
        if(type.type == DataTypes.ARRAY){
            ArrayDataType arrayDataType = (ArrayDataType) type;
            return new ArrayDataType(arrayDataType.name, arrayDataType.type, arrayDataType.itemType, arrayDataType.depth + 1);
        }
        return new DataType(type.name, type.type, type.depth + 1);
    }
    public static DataType getInt(){return new DataType("int", DataTypes.INT, 0);}
    public static DataType getDouble(){return new DataType("double", DataTypes.DOUBLE, 0);}
    public static DataType getLong(){return new DataType("long", DataTypes.LONG, 0);}
    public static DataType getShort(){return new DataType("short", DataTypes.SHORT, 0);}
    public static DataType getArray(DataType itemType){return new ArrayDataType("array", DataTypes.ARRAY, itemType, 0);}
    public static DataType getString(){return new DataType("string", DataTypes.STRING, 1);}
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
    public static DataType getHighestDataTypePrecedence(DataType l, DataType r){
        if(l.isPointer() || r.isPointer()){
            return l.isPointer() ? l : r;
        }
        if(l.isDouble() || r.isDouble()){
            return getFloat();
        }
        if(l.isFloat() || r.isFloat()){
            return getFloat();
        }
        if(l.isInteger() || r.isInteger()){
            return getInt();
        }
        if(l.isShort() || r.isShort()){
            return getShort();
        }
        return getByte();
    }

    public DataType(String name, DataTypes type, int depth){
        this.name = name;
        this.type = type;
        this.depth = depth;
    }
}
