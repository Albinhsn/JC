package se.liu.albhe576.project;

public enum Register {
    RAX,  RCX, RDX, RSI, RDI, R8, R9, RBP,RSP,
    EAX,  ECX,EDX, AX, CX, DX, AL, CL, DL,
    XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7;
    public static final Register PRIMARY_GENERAL_REGISTER= RAX;
    public static final Register SECONDARY_GENERAL_REGISTER= RCX;
    public static final Register PRIMARY_SSE_REGISTER= XMM0;
    public static final Register SECONDARY_SSE_REGISTER= XMM1;
    @Override
    public String toString() {return this.name().toLowerCase();}
    public static Register getPrimaryRegisterFromDataType(DataType type) throws CompileException {
        return getRegister(type, PRIMARY_SSE_REGISTER, RAX, EAX, AX, AL);
    }
    public static Register getSecondaryRegisterFromDataType(DataType type) throws CompileException {
        return getRegister(type, SECONDARY_SSE_REGISTER, RCX, ECX, CX, CL);
    }
    public static Register getThirdRegisterFromDataType(DataType type) throws CompileException {
        return getRegister(type, XMM2, RDX, EDX, DX, DL);
    }

    private static Register getRegister(DataType type, Register floatingPointRegister, Register longRegister, Register intRegister, Register shortRegister, Register byteRegister) throws CompileException {
        if(type.isPointer()){
            return longRegister;
        }
        switch(type.type){
            case DOUBLE,FLOAT -> {return floatingPointRegister;}
            case STRUCT, LONG, STRING -> {return longRegister;}
            case INT -> {return intRegister;}
            case SHORT -> {return shortRegister;}
            case BYTE -> {return byteRegister;}
        }
        throw new CompileException(String.format("Can't get Register %s", type));
    }

    public static Register getMinimumConvertSource(OperationType op, DataType source) throws CompileException {
        switch(op){
            case MOVSX, CVTSS2SI, CVTTSD2SI -> {return Register.getPrimaryRegisterFromDataType(source);}
            case CVTSS2SD, CVTSD2SS -> {
                return Register.XMM0;
            }
            case CVTSI2SS -> {
                return source.isLong() ? Register.RAX : Register.EAX;
            }
            case CVTSI2SD -> {
                return Register.RAX;
            }
        }
        throw new CompileException(String.format("Can't get convert target from %s", op.name()));
    }
    public static Register getMinimumConvertTarget(OperationType op, DataType target) throws CompileException {
        switch(op){
            case MOVSX -> {return Register.RAX;}
            case CVTSD2SS, CVTSI2SD, CVTSS2SD, CVTSI2SS -> {
                return Register.XMM0;
            }
            case CVTSS2SI, CVTTSD2SI -> {
                return target.isLong() ? Register.RAX : Register.EAX;
            }
        }
        throw new CompileException(String.format("Can't get convert target from %s", op.name()));
    }
}
