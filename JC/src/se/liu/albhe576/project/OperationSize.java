package se.liu.albhe576.project;

public enum OperationSize {
    QWORD, DWORD, WORD, BYTE;

    public static OperationSize getSizeFromRegister(Register register) throws CompileException {
        switch(register.type){
            case AL, BL, CL, DL -> {
                return BYTE;
            }
            case AX, BX, CX, DX -> {
                return WORD;
            }
            case EAX, EBX, ECX, EDX -> {
                return DWORD;
            }
            case RAX, RBX, RCX, RDX -> {
                return QWORD;
            }
        }
        throw new CompileException(String.format("Don't know how to get size from %s", register.type.name()));
    }
}
