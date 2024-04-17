package se.liu.albhe576.project;

public class PointerDataType extends DataType{
    public int depth;
    public PointerDataType(String name, DataTypes type, int depth){
        super(name, type);
        this.depth = depth;
    }

    @Override public DataType getTypeFromPointer() throws CompileException {

        return switch (type) {
            case INT_POINTER -> this.depth == 1 ? getInt() : getIntPointer(this.depth - 1);
            case FLOAT_POINTER -> this.depth == 1 ? getFloat() : getFloatPointer(this.depth - 1);
            case STRUCT_POINTER -> this.depth == 1 ? getStruct(name) : getStructPointer(name, this.depth - 1);
            default -> throw new CompileException(String.format("Can't get pointer from %s", name));
        };
    }
}
