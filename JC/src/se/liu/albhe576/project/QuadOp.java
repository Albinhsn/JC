package se.liu.albhe576.project;

import java.util.Map;

public enum QuadOp {
    ALLOCATE,CMP,JMP, MOV_REG_CA,
    MOV_XMM0, MOV_XMM1, MOV_XMM2, MOV_XMM3, MOV_XMM4, MOV_XMM5,
    MOV_R8, MOV_R9, MOV_RDX, MOV_RSI, MOV_RDI, MOV_RCX,
    STORE_INDEX, PUSH_STRUCT, INDEX, DEREFERENCE,LOAD_VARIABLE_POINTER, LOAD_POINTER,LOAD_FIELD_POINTER, GET_FIELD, SET_FIELD, MOVE_STRUCT, MOVE_ARG,
    CONVERT_LONG_TO_DOUBLE, CONVERT_DOUBLE_TO_LONG, CONVERT_DOUBLE_TO_FLOAT, CONVERT_FLOAT_TO_DOUBLE,
    ZX_BYTE, ZX_INT, ZX_SHORT, ZX_FLOAT,
    INC,DEC, ADD,SUB, MUL, DIV, SHL, SHR,MOD, IMUL,
    NEGATE, LOGICAL_NOT,AND, OR, XOR,
    SETNE, SETB, SETBE, SETA, SETAE, SETE, SETLE, SETGE, SETG, SETL,
    JL, JLE, JG, JGE, JNZ, JE, JA, JAE, JB, JBE,JNE,
    LABEL, POP,PUSH,CALL, RET,
    LOAD_IMM, LOAD, STORE;

    private static final Map<TokenType, QuadOp> QUAD_OP_FROM_TOKEN_MAP = Map.ofEntries(
            Map.entry(TokenType.TOKEN_PLUS, ADD),
            Map.entry(TokenType.TOKEN_AUGMENTED_PLUS, ADD),
            Map.entry(TokenType.TOKEN_MINUS, SUB),
            Map.entry(TokenType.TOKEN_AUGMENTED_MINUS, SUB),
            Map.entry(TokenType.TOKEN_STAR, MUL),
            Map.entry(TokenType.TOKEN_AUGMENTED_STAR, MUL),
            Map.entry(TokenType.TOKEN_SLASH, DIV),
            Map.entry(TokenType.TOKEN_AUGMENTED_SLASH,DIV),
            Map.entry(TokenType.TOKEN_AUGMENTED_XOR, XOR),
            Map.entry(TokenType.TOKEN_XOR, XOR),
            Map.entry(TokenType.TOKEN_AUGMENTED_OR, OR),
            Map.entry(TokenType.TOKEN_OR_BIT, OR),
            Map.entry(TokenType.TOKEN_AUGMENTED_AND, AND),
            Map.entry(TokenType.TOKEN_AND_BIT, AND),
            Map.entry(TokenType.TOKEN_SHIFT_LEFT, SHL),
            Map.entry(TokenType.TOKEN_SHIFT_RIGHT, SHR),
            Map.entry(TokenType.TOKEN_INCREMENT, INC),
            Map.entry(TokenType.TOKEN_DECREMENT, DEC),
            Map.entry(TokenType.TOKEN_BANG, LOGICAL_NOT),
            Map.entry(TokenType.TOKEN_BANG_EQUAL, SETNE),
            Map.entry(TokenType.TOKEN_LESS, SETL),
            Map.entry(TokenType.TOKEN_MOD, MOD),
            Map.entry(TokenType.TOKEN_LESS_EQUAL, SETLE),
            Map.entry(TokenType.TOKEN_GREATER, SETG),
            Map.entry(TokenType.TOKEN_EQUAL_EQUAL, SETE),
            Map.entry(TokenType.TOKEN_GREATER_EQUAL, SETGE)
    );
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

    public static QuadOp fromToken(Token token) throws CompileException {
        if(!QUAD_OP_FROM_TOKEN_MAP.containsKey(token.type())){
            throw new CompileException(String.format("Can't get quad op from this token %s", token.literal()));
        }
        return QUAD_OP_FROM_TOKEN_MAP.get(token.type());
    }

    public QuadOp getJmpFromSet() throws CompileException{
        if(!SET_TO_JMP_MAP.containsKey(this)){
            throw new CompileException(String.format("Can't transform set op %s to jump op?", this.name()));
        }
        return SET_TO_JMP_MAP.get(this);
    }

    public QuadOp invertJmpCondition() throws CompileException{
        if(!INVERT_JMP_MAP.containsKey(this)){
            throw new CompileException(String.format("Can't transform set op %s to jump op?", this.name()));
        }
        return INVERT_JMP_MAP.get(this);
    }

    public boolean isSet(){return this == SETNE || this == SETB || this == SETBE || this== SETA || this == SETAE || this == SETE || this == SETLE || this ==  SETGE || this == SETG || this== SETL;}
    public static boolean isBitwiseOp(QuadOp op){return op == AND || op == OR || op == XOR || op == SHL || op == SHR;}
    public static boolean isArithmeticOp(QuadOp op){return op == ADD || op == SUB || op == DIV || op == MOD || op == MUL;}
}
