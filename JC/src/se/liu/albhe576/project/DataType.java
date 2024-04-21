package se.liu.albhe576.project;

public class DataType {

    @Override
    public String toString() {
        return String.format("%s %s %d", name, type.name(), depth);
    }

    protected final String name;
    protected final DataTypes type;
    protected final int depth;

    public boolean isFloatingPoint(){
        return this.type == DataTypes.FLOAT && depth == 0;
    }
    public boolean isInteger(){
        return this.type == DataTypes.INT && depth == 0;
    }
    public boolean isStruct(){
        return this.type == DataTypes.STRUCT && depth == 0;
    }
    public boolean isArray(){
        return this.type == DataTypes.ARRAY && depth == 0;
    }
    public boolean isString(){
        return this.type == DataTypes.STRING && depth == 0;
    }

    public boolean isPointer(){
        return this.depth > 0;
    }

    public boolean isSameType(DataType other){
        if(other.isStruct() && this.isStruct()){
            return other.name.equals(name);
        }

        return ((type == other.type && depth == 0 && other.depth == 0)) || (depth > 0 && other.depth > 0);
    }
    public boolean canBeCastedTo(DataType other){
        boolean same = this.isSameType(other);

        if((this.isFloatingPoint() && other.isInteger()) || (other.isFloatingPoint() && this.isInteger())){
            return true;
        }

        if((this.isArray() && (other.isPointer() && other.depth == 1)) || (other.isArray() && (this.isPointer() && this.depth == 1))){
            return true;
        }
        return same;
    }

    public DataType getTypeFromPointer() throws CompileException {
        if(depth <= 0){
            throw new CompileException("How could this happen to me, depth < 0?");
        }
        if(this.type == DataTypes.STRING && this.depth == 1){
            return new DataType("int", DataTypes.INT, 0);
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
    public static DataType getInt(){
        return new DataType("int", DataTypes.INT, 0);
    }
    public static DataType getArray(DataType itemType){
        return new ArrayDataType("array", DataTypes.ARRAY, itemType, 0);
    }
    public static DataType getString(){
        return new DataType("string", DataTypes.STRING, 1);
    }
    public static DataType getFloat(){
        return new DataType("float", DataTypes.FLOAT, 0);
    }
    public static DataType getStruct(String name){
        return new DataType(name, DataTypes.STRUCT, 0);
    }
    public static DataType getVoid(){
        return new DataType("void", DataTypes.VOID, 0);
    }

    public static DataType getDataTypeFromToken(Token token) throws CompileException {
        switch(token.type()){
            case TOKEN_INT, TOKEN_INT_LITERAL -> {return DataType.getInt();}
            case TOKEN_FLOAT, TOKEN_FLOAT_LITERAL -> {return DataType.getFloat();}
            case TOKEN_STRING_LITERAL, TOKEN_STRING-> {return DataType.getString();}
            case TOKEN_IDENTIFIER -> {return getStruct(token.literal());}
            case TOKEN_VOID -> {return getVoid();}
        }
        throw new CompileException(String.format("Can't parse value type from %s", token));
    }

    public DataType(String name, DataTypes type, int depth){
        this.name = name;
        this.type = type;
        this.depth = depth;
    }
}
