package se.liu.albhe576.project;

public class DataType {

    public String name;
    public DataTypes type;

    public boolean isSameType(DataType other){
        if((other.type == DataTypes.STRUCT && type == DataTypes.STRUCT) || (other.type == DataTypes.STRUCT_POINTER && type == DataTypes.STRUCT_POINTER)){
            return other.name.equals(name);
        }
        return type == other.type;
    }

    public static DataType getPointerType(DataType dataType) throws UnexpectedTokenException {
        switch(dataType.type){
            case INT -> {return getIntPointer();}
            case FLOAT -> {return getFloatPointer();}
            case BYTE -> {return getBytePointer();}
            case STRUCT -> {return getStructPointer(dataType.name);}
        }
        throw new UnexpectedTokenException(String.format("Can't parse value type pointer from %s", dataType.name));
    }
    public static DataType getFunction(){
        return new DataType("function", DataTypes.FUNCTION);
    }
    public static DataType getInt(){
        return new DataType("int", DataTypes.INT);
    }
    public static DataType getIntPointer(){
        return new DataType("int *", DataTypes.INT_POINTER);
    }
    public static DataType getFloatPointer(){
        return new DataType("float *", DataTypes.FLOAT_POINTER);
    }
    public static DataType getBytePointer(){
        return new DataType("byte *", DataTypes.BYTE_POINTER);
    }
    public static DataType getStructPointer(String name){
        return new DataType(name + " *", DataTypes.STRUCT_POINTER);
    }
    public static DataType getFloat(){
        return new DataType("float", DataTypes.FLOAT);
    }
    public static DataType getByte(){
        return new DataType("byte", DataTypes.BYTE);
    }
    public static DataType getStruct(String name){
        return new DataType(name, DataTypes.STRUCT);
    }
    public static DataType getVoid(){
        return new DataType("void", DataTypes.VOID);
    }

    public static DataType getDataTypeFromToken(Token token) throws UnexpectedTokenException {
        switch(token.type){
            case TOKEN_INT, TOKEN_INT_LITERAL -> {return DataType.getInt();}
            case TOKEN_FLOAT, TOKEN_FLOAT_LITERAL -> {return DataType.getFloat();}
            case TOKEN_BYTE -> {return getByte();}
            case TOKEN_IDENTIFIER -> {return getStruct(token.literal);}
            case TOKEN_VOID -> {return getVoid();}
        }
        throw new UnexpectedTokenException(String.format("Can't parse value type from %s", token));
    }

    public DataType(String name, DataTypes type){
        this.name = name;
        this.type = type;
    }
}
