package se.liu.albhe576.project;

public enum DataTypes {
    STRING, INT, FLOAT, INT_POINTER, FLOAT_POINTER, STRUCT, STRUCT_POINTER, VOID, VOID_POINTER, ARRAY;

    public boolean isFloat(){
        return this == DataTypes.FLOAT;
    }
    public boolean isInteger(){
        return this == DataTypes.INT;
    }
    public boolean isStruct(){
        return this == DataTypes.STRUCT || this == DataTypes.STRUCT_POINTER;
    }
    public boolean isPointer(){
        switch(this){
            case INT_POINTER:{}
            case FLOAT_POINTER:{}
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
