package se.liu.albhe576.project;

public record Token(TokenType type, int line, String literal) {
    @Override
    public String toString() {
        return String.format("%s: %s at %d", type, literal, line);
    }

}
