package se.liu.albhe576.project;

public enum QuadOp {
    INDEX, POP, LOAD_IMM, INC, ADD, JG, JGE, JL, SUB, MUL, DIV, LOAD, STORE, CMP, JMP, JNZ, JZ, AND, OR, XOR, PUSH, CALL, GET_FIELD, SET_FIELD, MOV_REG_AC, MOV_REG_CA, RET, SHL, SHR;

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
            case TOKEN_LESS-> {
                return JL;
            }
            case TOKEN_GREATER-> {
                return JG;
            }
            case TOKEN_GREATER_EQUAL-> {
                return JGE;
            }
            case TOKEN_SHIFT_RIGHT-> {
                return SHR;
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
