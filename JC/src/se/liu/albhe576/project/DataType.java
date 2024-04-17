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

    public int getSize(){
        if(type.isStruct()){

        }
        return 0;
    }


    public DataType getTypeFromPointer() throws CompileException {
        return switch (type) {
            case INT_POINTER -> getInt();
            case FLOAT_POINTER -> getFloat();
            case STRUCT_POINTER -> getStruct(name);
            default -> throw new CompileException(String.format("Can't get pointer from %s", name));
        };
    }
    public static PointerDataType getPointerFromType(DataType type) throws CompileException {
            return switch (type.type) {
                case INT -> DataType.getIntPointer(1);
                case INT_POINTER -> {
                   PointerDataType dataType = (PointerDataType)  type;
                   yield DataType.getIntPointer(dataType.depth + 1);
                }
                case FLOAT -> getFloatPointer(1);
                case FLOAT_POINTER -> {
                    PointerDataType dataType = (PointerDataType)  type;
                    yield getFloatPointer(dataType.depth + 1);
                }
                case STRUCT -> getStructPointer(type.name, 1);
                case STRUCT_POINTER -> {
                    PointerDataType dataType = (PointerDataType)  type;
                    yield getStructPointer(type.name, dataType.depth + 1);
                }
                default -> throw new CompileException(String.format("Can't get pointer from %s", type.name));
            };
    }
    public static DataType getInt(){
        return new DataType("int", DataTypes.INT);
    }
    public static PointerDataType getIntPointer(int depth){
        return new PointerDataType("int", DataTypes.INT_POINTER, depth);
    }
    public static PointerDataType getFloatPointer(int depth){
        return new PointerDataType("float", DataTypes.FLOAT_POINTER, depth);
    }
    public static DataType getVoidPointer(){
        return new DataType("void", DataTypes.VOID_POINTER);
    }
    public static DataType getString(){
        return new DataType("string", DataTypes.STRING);
    }
    public static PointerDataType getStructPointer(String name, int depth){
        return new PointerDataType(name, DataTypes.STRUCT_POINTER, depth);
    }
    public static DataType getFloat(){
        return new DataType("float", DataTypes.FLOAT);
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
            case TOKEN_STRING-> {return DataType.getString();}
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
