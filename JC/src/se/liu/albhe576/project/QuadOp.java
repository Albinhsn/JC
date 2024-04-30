package se.liu.albhe576.project;

import java.util.Map;

public enum QuadOp {
    JMP, LOAD_POINTER,CONVERT, PARAM, MEMBER, INDEX, REFERENCE, DEREFERENCE, JMP_T, JMP_F,
    INC_I,DEC_I, ADD_I,SUB_I, MUL_I, DIV_I, SHL, SHR,MOD,
    INC_F,DEC_F, ADD_F,SUB_F, MUL_F, DIV_F,
    NEGATE, AND, OR, XOR,
    LESS_I, LESS_EQUAL_I, GREATER_I, GREATER_EQUAL_I, EQUAL_I,NOT_EQUAL_I,NOT_I ,
    LESS_F, LESS_EQUAL_F, GREATER_F, GREATER_EQUAL_F, EQUAL_F,NOT_EQUAL_F, NOT_F,
    LOGICAL_AND, LOGICAL_OR,
    LABEL, CALL,
    RET_I, RET_F, LOAD_IMM_I, LOAD_IMM_F, LOAD_I, STORE_I, LOAD_F, STORE_F;

    private static final Map<TokenType, QuadOp> TOKEN_TO_QUAD_OP_MAP = Map.ofEntries(
            Map.entry(TokenType.TOKEN_PLUS, ADD_I),
            Map.entry(TokenType.TOKEN_MINUS, SUB_I),
            Map.entry(TokenType.TOKEN_STAR, MUL_I),
            Map.entry(TokenType.TOKEN_SLASH, DIV_I),
            Map.entry(TokenType.TOKEN_AUGMENTED_PLUS, ADD_I),
            Map.entry(TokenType.TOKEN_AUGMENTED_MINUS, SUB_I),
            Map.entry(TokenType.TOKEN_AUGMENTED_STAR, MUL_I),
            Map.entry(TokenType.TOKEN_AUGMENTED_SLASH,DIV_I),
            Map.entry(TokenType.TOKEN_AUGMENTED_XOR, XOR),
            Map.entry(TokenType.TOKEN_AUGMENTED_OR, OR),
            Map.entry(TokenType.TOKEN_AUGMENTED_AND, AND),
            Map.entry(TokenType.TOKEN_XOR, XOR),
            Map.entry(TokenType.TOKEN_OR_BIT, OR),
            Map.entry(TokenType.TOKEN_AND_BIT, AND),
            Map.entry(TokenType.TOKEN_SHIFT_LEFT, SHL),
            Map.entry(TokenType.TOKEN_SHIFT_RIGHT, SHR),
            Map.entry(TokenType.TOKEN_INCREMENT, INC_I),
            Map.entry(TokenType.TOKEN_DECREMENT, DEC_I),
            Map.entry(TokenType.TOKEN_BANG, NOT_I),
            Map.entry(TokenType.TOKEN_BANG_EQUAL, NOT_EQUAL_I),
            Map.entry(TokenType.TOKEN_LESS, LESS_I),
            Map.entry(TokenType.TOKEN_MOD, MOD),
            Map.entry(TokenType.TOKEN_LESS_EQUAL, LESS_EQUAL_I),
            Map.entry(TokenType.TOKEN_GREATER, GREATER_I),
            Map.entry(TokenType.TOKEN_EQUAL_EQUAL, EQUAL_I),
            Map.entry(TokenType.TOKEN_GREATER_EQUAL, GREATER_EQUAL_I)
    );
    public static QuadOp fromToken(TokenType type) throws CompileException {
        if(!TOKEN_TO_QUAD_OP_MAP.containsKey(type)){
            throw new CompileException(String.format("Can't convert %s to quad op", type.name()));
        }
        return TOKEN_TO_QUAD_OP_MAP.get(type);
    }
    private static final Map<QuadOp, QuadOp> INT_QUAD_TO_FLOAT = Map.ofEntries(
            Map.entry(ADD_I, ADD_F),
            Map.entry(SUB_I, SUB_F),
            Map.entry(MUL_I, MUL_F),
            Map.entry(DIV_I, DIV_F),
            Map.entry(INC_I, INC_F),
            Map.entry(DEC_I, DEC_F),
            Map.entry(NOT_I, NOT_F),
            Map.entry(NOT_EQUAL_I, NOT_EQUAL_F),
            Map.entry(LESS_I, LESS_F),
            Map.entry(LESS_EQUAL_I, LESS_EQUAL_F),
            Map.entry(GREATER_I, GREATER_F),
            Map.entry(EQUAL_I, EQUAL_F),
            Map.entry(GREATER_EQUAL_I, GREATER_EQUAL_F)
    );
    public QuadOp convertToFloat() throws CompileException {
        if(!INT_QUAD_TO_FLOAT.containsKey(this)){
            throw new CompileException(String.format("Can't convert %s to float op", this.name()));
        }
        return INT_QUAD_TO_FLOAT.get(this);
    }
    public boolean isBitwise(){
        return this == AND || this == OR || this == XOR || this == SHL || this == SHR;
    }
}
