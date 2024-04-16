package se.liu.albhe576.project;

public enum DataTypes {
    INT, FLOAT, BYTE, INT_POINTER, FLOAT_POINTER, BYTE_POINTER, STRUCT, STRUCT_POINTER, VOID, VOID_POINTER, FUNCTION;

    public boolean isInteger(){
        return this == DataTypes.INT || this == DataTypes.BYTE;
    }
    public boolean isPointer(){
        switch(this){
            case INT_POINTER:{}
            case FLOAT_POINTER:{}
            case BYTE_POINTER:{}
            case VOID_POINTER:{}
            case STRUCT_POINTER:{
                return true;
            }
            default:{
                return false;
            }
        }
    }

}
