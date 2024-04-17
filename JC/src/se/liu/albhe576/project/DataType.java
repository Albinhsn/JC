package se.liu.albhe576.project;

public class DataType {

    public String name;
    public DataTypes type;

    public boolean isSameType(DataType other){
        if(other.type.isStruct()  && type.isStruct()){
            return other.name.equals(name);
        }

        return (type == other.type) || (type.isPointer() && other.type.isPointer());
    }


    public static DataType getTypeFromPointer(DataType type) throws CompileException {
        return switch (type.type) {
            case INT_POINTER -> getInt();
            case FLOAT_POINTER -> getFloat();
            case BYTE_POINTER -> getByte();
            case STRUCT_POINTER -> getStruct(type.name);
            default -> throw new CompileException(String.format("Can't get pointer from %s", type.name));
        };
    }
    public static DataType getPointerFromType(DataType type) throws CompileException {
            return switch (type.type) {
                case INT, INT_POINTER -> DataType.getIntPointer();
                case FLOAT, FLOAT_POINTER -> getFloatPointer();
                case BYTE, BYTE_POINTER -> getBytePointer();
                case STRUCT, STRUCT_POINTER -> getStructPointer(type.name);
                default -> throw new CompileException(String.format("Can't get pointer from %s", type.name));
            };
    }
    public static DataType getFunction(){
        return new DataType("function", DataTypes.FUNCTION);
    }
    public static DataType getInt(){
        return new DataType("int", DataTypes.INT);
    }
    public static DataType getIntPointer(){
        return new DataType("int", DataTypes.INT_POINTER);
    }
    public static DataType getFloatPointer(){
        return new DataType("float", DataTypes.FLOAT_POINTER);
    }
    public static DataType getBytePointer(){
        return new DataType("byte", DataTypes.BYTE_POINTER);
    }
    public static DataType getVoidPointer(){
        return new DataType("void", DataTypes.VOID_POINTER);
    }
    public static DataType getStructPointer(String name){
        return new DataType(name, DataTypes.STRUCT_POINTER);
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
            case TOKEN_STRING-> {return getBytePointer();}
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
