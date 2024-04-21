package se.liu.albhe576.project;

import java.util.Map;

public enum QuadOp {

    ALLOCATE,
    MOV_XMM0, MOV_XMM1, MOV_XMM2, MOV_XMM3, MOV_XMM4, MOV_XMM5, SWAPF,
    MOV_R8, MOV_R9, MOV_RDX, MOV_RSI, MOV_RDI, MOV_RCX,
    STORE_INDEX, PUSH_STRUCT, INDEX, DEREFERENCE,LOAD_POINTER,GET_FIELD, SET_FIELD, MOVE_STRUCT, MOVE_ARG,
    CVTSI2SD, CVTTSD2SI,
     FADD, FMUL, FDIV, FSUB,INC,DEC, ADD,SUB, MUL, DIV, SHL, SHR,MOD, IMUL,
    NEGATE, LOGICAL_NOT,AND, OR, XOR,
    SETNE, SETB, SETBE, SETA, SETAE, SETE, SETLE, SETGE, SETG, SETL,
    JL, JLE, JG, JGE, JNZ, JE, JA, JAE, JB, JBE,JNE,
    CMP,JMP,
    LABEL, POP,PUSH,CALL, RET,
     LOAD_IMM, LOAD, STORE,
    MOV_REG_AC, MOV_REG_CA;

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

    public QuadOp convertToFloat() throws CompileException {
        switch(this){
            case ADD, FADD -> {return FADD;}
            case SUB, FSUB -> {return FSUB;}
            case DIV, FDIV -> {return FDIV;}
            case MUL, FMUL -> {return FMUL;}
        }

        throw new CompileException(String.format("Can't convert op %s to float op?", this.name()));
    }

    public boolean isSet(){
        return this == SETNE || this == SETB || this == SETBE || this== SETA || this == SETAE || this == SETE || this == SETLE || this ==  SETGE || this == SETG || this== SETL;
    }
    private static final Map<QuadOp, QuadOp> SET_TO_JMP_MAP = Map.of(
            SETNE, JNE,
            SETB, JB,
            SETBE, JBE,
            SETA, JA,
            SETAE, JAE,
            SETE, JE,
            SETLE, JLE,
            SETGE, JGE,
            SETG, JG,
            SETL, JL
    );
    public QuadOp getJmpFromSet() throws CompileException{
        if(!SET_TO_JMP_MAP.containsKey(this)){
            throw new CompileException(String.format("Can't transform set op %s to jump op?", this.name()));
        }
        return SET_TO_JMP_MAP.get(this);
    }

    private static final Map<QuadOp, QuadOp> INVERT_JMP_MAP = Map.of(
             JNE, JE,
             JB, JAE,
             JBE, JA,
             JA, JBE,
             JAE, JB,
             JE, JNE,
             JLE, JG,
             JGE, JL,
             JG, JLE,
             JL, JGE
    );
    public QuadOp invertJmpCondition() throws CompileException{
        if(!INVERT_JMP_MAP.containsKey(this)){
            throw new CompileException(String.format("Can't transform set op %s to jump op?", this.name()));
        }
        return INVERT_JMP_MAP.get(this);
    }
}
