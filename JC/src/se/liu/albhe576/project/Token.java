package se.liu.albhe576.project;

public class Token {

    @Override
    public String toString() {
        return String.format("%s: %s at %d", type, literal, line);
    }

    public TokenType type;
    public final int line;
    public String literal;

    public Token(TokenType type, int line, String literal){
        this.type       = type;
        this.line       = line;
        this.literal    = literal;
    }
}
