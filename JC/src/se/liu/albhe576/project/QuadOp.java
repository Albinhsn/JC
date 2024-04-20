package se.liu.albhe576.project;

public enum QuadOp {

    ALLOCATE,
    MOV_XMM0, MOV_XMM1, MOV_XMM2, MOV_XMM3, MOV_XMM4, MOV_XMM5, SWAPF,
    MOV_R8, MOV_R9, MOV_RDX, MOV_RSI, MOV_RDI, MOV_RCX,
    STORE_INDEX, PUSH_STRUCT, INDEX, DEREFERENCE,LOAD_POINTER,GET_FIELD, SET_FIELD, MOVE_STRUCT, MOVE_ARG,
    CVTSI2SD, CVTTSD2SI,
     FADD, FMUL, FDIV, FSUB,INC,DEC, ADD,SUB, MUL, DIV,SAL, SAR,MOD, IMUL,
    NOT, LOGICAL_NOT,AND, OR, XOR,
    SETNE, SETB, SETBE, SETA, SETAE, SETE, CMP, JMP, JNZ, JE,
    SETLE, SETGE, SETG, SETL,
    LABEL, POP,PUSH,CALL, RET,
     LOAD_IMM, LOAD, STORE,
    MOV_REG_AC, MOV_REG_CA;

    public boolean isLoad(){
        return this == LOAD || this == LOAD_IMM || this == LOAD_POINTER;
    }

    public static QuadOp fromToken(Token token) throws CompileException {
        switch(token.type){
            case TOKEN_PLUS, TOKEN_AUGMENTED_PLUS -> {
                return ADD;
            }
            case TOKEN_MINUS, TOKEN_AUGMENTED_MINUS -> {
                return SUB;
            }
            case TOKEN_STAR, TOKEN_AUGMENTED_STAR -> {
                return MUL;
            }
            case TOKEN_SLASH, TOKEN_AUGMENTED_SLASH -> {
                return DIV;
            }
            case TOKEN_SHIFT_LEFT-> {
                return SAL;
            }
            case TOKEN_SHIFT_RIGHT-> {
                return SAR;
            }
            case TOKEN_INCREMENT-> {
                return INC;
            }
            case TOKEN_DECREMENT-> {
                return DEC;
            }
            case TOKEN_BANG-> {
                return LOGICAL_NOT;
            }
            case TOKEN_BANG_EQUAL-> {
                return SETNE;
            }
            case TOKEN_LESS-> {
                return SETL;
            }
            case TOKEN_LESS_EQUAL-> {
                return SETLE;
            }
            case TOKEN_GREATER-> {
                return SETG;
            }
            case TOKEN_EQUAL_EQUAL-> {
                return SETE;
            }
            case TOKEN_GREATER_EQUAL-> {
                return SETGE;
            }
            case TOKEN_AUGMENTED_AND, TOKEN_AND_BIT -> {
                return AND;
            }
            case TOKEN_AUGMENTED_OR, TOKEN_OR_BIT-> {
                return OR;
            }
            case TOKEN_AUGMENTED_XOR, TOKEN_XOR-> {
                return XOR;
            }
            case TOKEN_MOD-> {
                return MOD;
            }
        }
        throw new CompileException(String.format("Unknown token to quad op? %s", token.literal));
    }

    public QuadOp convertToFloat() throws CompileException {
        switch(this){
            case ADD, FADD -> {return FADD;}
            case SUB, FSUB -> {return FSUB;}
            case DIV, FDIV -> {return FDIV;}
            case MUL, FMUL -> {return FMUL;}
        }

        throw new CompileException(String.format("Can't convert op %s to float op?", this.name()));
    }
}
