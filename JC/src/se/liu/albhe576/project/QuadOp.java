package se.liu.albhe576.project;

public enum QuadOp {
    MOV_XMM0, MOV_XMM1, MOV_XMM2, MOV_XMM3, MOV_XMM4, MOV_XMM5, STORE_INDEX, PUSH_STRUCT, CVTSI2SD, CVTTSD2SI, MOD, MOV_R8, MOV_R9, MOV_RDX, MOV_RSI, MOV_RDI, DEREFERENCE, FADD, FMUL, FDIV, FSUB, NOT, LOGICAL_NOT, SETNE, SETLE, SETL, SETG, SETGE, SETE, LABEL, INDEX, POP, LOAD_POINTER, LOAD_IMM, INC,DEC, ADD, JE, SUB, MUL, DIV, LOAD, STORE, CMP, JMP, JNZ, AND, OR, XOR, PUSH,CALL, GET_FIELD, SET_FIELD, MOV_REG_AC, MOV_REG_CA, RET, SHL, SHR, MOVE_STRUCT;

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
                return SHL;
            }
            case TOKEN_SHIFT_RIGHT-> {
                return SHR;
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

    public QuadOp convertToFloat() throws InvalidOperation {
        switch(this){
            case ADD, FADD -> {return FADD;}
            case SUB, FSUB -> {return FSUB;}
            case DIV, FDIV -> {return FDIV;}
            case MUL, FMUL -> {return FMUL;}
        }

        throw new InvalidOperation(String.format("Can't convert op %s to float op?", this.name()));
    }
}
