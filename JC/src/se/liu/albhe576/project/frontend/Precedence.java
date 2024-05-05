package se.liu.albhe576.project.frontend;

/**
 * The different precedence levels within the language
 * These can be used with their corresponding ordinal values in other to determine their precedence
 * @see Parser
 */
public enum Precedence {
    NONE, ASSIGNMENT, OR, AND, EQUALITY, COMPARISON, BITWISE, TERM, FACTOR, UNARY, CALL, PRIMARY
}
