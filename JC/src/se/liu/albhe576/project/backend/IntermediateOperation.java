package se.liu.albhe576.project.backend;

import java.util.Map;

import se.liu.albhe576.project.frontend.*;

/**
 * The different intermediates used for intermediate code generation
 * Is generic higher level version of assembly instructions
 * @see Intermediate
 * @see IntermediateList
 */
public enum IntermediateOperation
{
    JMP, LOAD_MEMBER_POINTER, LOAD_POINTER, CONVERT, LOAD_MEMBER, INDEX, DEREFERENCE, JMP_T, JMP_F, REFERENCE_INDEX, ASSIGN, IMUL, CAST,
    ADD,SUB, MUL, DIV, SHL, SHR,MOD,PRE_INC, PRE_DEC, POST_INC, POST_DEC,
    NEGATE, AND, OR, XOR, LOGICAL_OR, LOGICAL_AND,
    LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL,NOT_EQUAL,NOT,
    LABEL, CALL, RET, LOAD_IMM, LOAD, STORE_ARRAY_ITEM;

    private static final Map<TokenType, IntermediateOperation> TOKEN_TO_INTERMEDIATE_OP_MAP = Map.ofEntries(
            Map.entry(TokenType.TOKEN_PLUS, ADD),
            Map.entry(TokenType.TOKEN_MINUS, SUB),
            Map.entry(TokenType.TOKEN_STAR, MUL),
            Map.entry(TokenType.TOKEN_SLASH, DIV),
            Map.entry(TokenType.TOKEN_AND_LOGICAL, LOGICAL_AND),
            Map.entry(TokenType.TOKEN_OR_LOGICAL, LOGICAL_OR),
            Map.entry(TokenType.TOKEN_AUGMENTED_PLUS, ADD),
            Map.entry(TokenType.TOKEN_AUGMENTED_MINUS, SUB),
            Map.entry(TokenType.TOKEN_AUGMENTED_STAR, MUL),
            Map.entry(TokenType.TOKEN_AUGMENTED_SLASH,DIV),
            Map.entry(TokenType.TOKEN_AUGMENTED_XOR, XOR),
            Map.entry(TokenType.TOKEN_AUGMENTED_OR, OR),
            Map.entry(TokenType.TOKEN_AUGMENTED_AND, AND),
            Map.entry(TokenType.TOKEN_XOR, XOR),
            Map.entry(TokenType.TOKEN_OR_BIT, OR),
            Map.entry(TokenType.TOKEN_AND_BIT, AND),
            Map.entry(TokenType.TOKEN_SHIFT_LEFT, SHL),
            Map.entry(TokenType.TOKEN_SHIFT_RIGHT, SHR),
            Map.entry(TokenType.TOKEN_BANG, NOT),
            Map.entry(TokenType.TOKEN_BANG_EQUAL, NOT_EQUAL),
            Map.entry(TokenType.TOKEN_LESS, LESS),
            Map.entry(TokenType.TOKEN_MOD, MOD),
            Map.entry(TokenType.TOKEN_LESS_EQUAL, LESS_EQUAL),
            Map.entry(TokenType.TOKEN_GREATER, GREATER),
            Map.entry(TokenType.TOKEN_EQUAL_EQUAL, EQUAL),
            Map.entry(TokenType.TOKEN_GREATER_EQUAL, GREATER_EQUAL)
    );
    public static IntermediateOperation fromToken(TokenType type) throws CompileException {
        if(!TOKEN_TO_INTERMEDIATE_OP_MAP.containsKey(type)){
            throw new CompileException(String.format("Can't convert %s to intermediate op", type.name()));
        }
        return TOKEN_TO_INTERMEDIATE_OP_MAP.get(type);
    }
    public boolean isBitwise(){
        return this == AND || this == OR || this == XOR || this == SHL || this == SHR;
    }
}
