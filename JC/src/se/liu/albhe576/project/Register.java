package se.liu.albhe576.project;

public enum Register {
    RAX,  RCX, RDX, RSI, RDI, R8, R9, RBP,RSP,
    EAX,  ECX,EDX, AX, CX, DX, AL, CL, DL,
    XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7;
    public static final Register PRIMARY_GENERAL_REGISTER = RAX;
    public static final Register SECONDARY_GENERAL_REGISTER = RCX;
    public static final Register PRIMARY_SSE_REGISTER = XMM0;
    public static final Register SECONDARY_SSE_REGISTER = XMM1;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

    public static Register getPrimaryRegisterFromDataType(DataType type){

        if(type.isFloatingPoint()){
            return PRIMARY_SSE_REGISTER;
        }
        if(type.isStruct() || type.isPointer() || type.isLong()){
            return RAX;
        }
        else if(type.isInt()){
            return EAX;
        }
        else if(type.isShort()){
            return AX;
        }
        return AL;
    }
    public static Register getSecondaryRegisterFromDataType(DataType type){
        if(type.isFloatingPoint()){
            return SECONDARY_SSE_REGISTER;
        }
        if(type.isStruct() || type.isPointer() || type.isLong()){
            return RCX;
        }
        else if(type.isInt()){
            return ECX;
        }
        else if(type.isShort()){
            return CX;
        }
        return CL;
    }
    public static Register getThirdRegisterFromDataType(DataType type){
        if(type.isFloatingPoint()){
            return XMM2;
        }
        if(type.isPointer() || type.isLong()){
            return RDX;
        }
        else if(type.isInt()){
            return EDX;
        }
        else if(type.isShort()){
            return DX;
        }
        return DL;
    }
}
