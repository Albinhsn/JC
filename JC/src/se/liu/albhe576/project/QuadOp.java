package se.liu.albhe576.project;

public enum QuadOp {
    NOT, LOGICAL_NOT, SETNE, SETLE, SETL, SETG, SETGE, SETE, LABEL, INDEX, POP, LOAD_POINTER, LOAD_IMM, INC,DEC, ADD, JG,JE, JGE, JL, SUB, MUL, DIV, LOAD, STORE, CMP, JMP, JNZ, JZ, AND, OR, XOR, PUSH,CALL, GET_FIELD, SET_FIELD, MOV_REG_AC, MOV_REG_CA, MOV_REG_AD, MOV_REG_DA, RET, SHL, SHR;

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
        }
        throw new CompileException(String.format("Unknown token to quad op? %s", token.literal));
    }
}
