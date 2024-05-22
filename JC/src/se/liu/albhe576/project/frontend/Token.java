package se.liu.albhe576.project.frontend;

/**
 * A Token or lexical token is a symbol that refers to something more specific within a programming language
 * Is created by the scanner and sent to the parser for semantic analysis
 * @param type the type of token
 * @param literal the literal value of the token
 * @see Scanner
 * @see Parser
 */
public record Token(TokenType type, String literal) {
    @Override
    public String toString() {
        return String.format("%s: %s", type, literal);
    }

}
