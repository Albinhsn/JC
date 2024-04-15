package se.liu.albhe576.project;

public enum StructType {
    INT, FLOAT, BYTE, INT_POINTER, FLOAT_POINTER, BYTE_POINTER, STRUCT, STRUCT_POINTER, VOID;

    public static StructType getPointerType(StructType type) throws UnexpectedTokenException {
        switch(type){
            case INT -> {return INT_POINTER;}
            case FLOAT -> {return FLOAT_POINTER;}
            case BYTE -> {return BYTE_POINTER;}
            case STRUCT -> {return STRUCT_POINTER;}
        }
        throw new UnexpectedTokenException(String.format("Can't parse value type pointer from %s", type));
    }
    public static StructType getStructTypeFromToken(Token token) throws UnexpectedTokenException {
        switch(token.type){
            case TOKEN_INT, TOKEN_INT_LITERAL -> {return INT;}
            case TOKEN_FLOAT, TOKEN_FLOAT_LITERAL -> {return FLOAT;}
            case TOKEN_BYTE -> {return BYTE;}
            case TOKEN_IDENTIFIER -> {return STRUCT;}
            case TOKEN_VOID -> {return VOID;}
        }
        throw new UnexpectedTokenException(String.format("Can't parse value type from %s", token));
    }
}
