package se.liu.albhe576.project;

import java.util.Map;

public class Scanner {
    private final String input;
    private int index;
    private int line;

    public int getLine(){
        return this.line;
    }
    private char getCurrentChar(){
        if(index >= input.length()){
            return 0;
        }
        return input.charAt(index);
    }

    public void skipWhiteSpace(){
        for(;;){
            char currentChar = this.getCurrentChar();
            if(currentChar == 0 || Character.isDigit(currentChar) || Character.isAlphabetic(currentChar)){
                return;
            }
            switch(currentChar){
                case '/':{
                    advance();
                    if(this.matchNext('/')){
                        while(!this.isOutOfBounds() && this.getCurrentChar() != '\n'){
                            advance();
                        }
                        line++;
                        break;
                    }
                    this.index--;
                }
                case '\"':
                case '{':
                case '[':
                case ']':
                case '}':
                case '*':
                case ';':
                case '<':
                case '>':
                case '(':
                case ')':
                case '!':
                case '%':
                case '.':
                case '#':
                case '^':
                case ',':
                case '&':
                case '|':
                case '-':
                case '+':
                case '=':{
                    return;
                }
                case '\n':{
                    this.line++;
                    break;
                }
            }
            index++;
        }
    }

    private Token parseString() throws CompileException{

        int startIndex = this.index;
        int line = this.line;

        char currentChar = advance();
        while(!isOutOfBounds() && currentChar != '\"'){
            currentChar = advance();
        }

        if(isOutOfBounds()){
            throw new CompileException(String.format("Unterminated string at starting at line %d", line));
        }

        String literal = this.input.substring(startIndex, this.index - 1);
        return createToken(TokenType.TOKEN_STRING_LITERAL, literal);
    }
    public TokenType getKeyword(String literal){
    final Map<String, TokenType> reservedWords = Map.ofEntries(
                Map.entry("struct", TokenType.TOKEN_STRUCT),
                Map.entry("string", TokenType.TOKEN_STRING),
                Map.entry("if", TokenType.TOKEN_IF),
                Map.entry("else", TokenType.TOKEN_ELSE),
                Map.entry("for", TokenType.TOKEN_FOR),
                Map.entry("while", TokenType.TOKEN_WHILE),
                Map.entry("return", TokenType.TOKEN_RETURN),
                Map.entry("int", TokenType.TOKEN_INT),
                Map.entry("byte", TokenType.TOKEN_BYTE),
                Map.entry("float", TokenType.TOKEN_FLOAT),
                Map.entry("void", TokenType.TOKEN_VOID)
        );
        if(reservedWords.containsKey(literal)){
            return reservedWords.get(literal);
        }
        return TokenType.TOKEN_IDENTIFIER;
    }

    private Token parseKeyword() {
        int startIndex = this.index - 1;
        char currentChar = advance();
        while(!isOutOfBounds() && (Character.isDigit(currentChar)|| Character.isAlphabetic(currentChar) || currentChar == '_')){
            currentChar = advance();
        }

        this.index--;
        String literal = this.input.substring(startIndex, this.index);
        TokenType keyword = getKeyword(literal);
        return createToken(keyword, literal);
    }

    private boolean isValidHexChar(char c){
        return ('a' <= c &&  c <= 'f') || ('A' <= c && c <= 'F');
    }

    private Token parseHex(int startIndex){
        char currentChar = advance();
        while(!isOutOfBounds() && (Character.isDigit(currentChar) || (this.isValidHexChar(currentChar)))){
            currentChar = advance();
        }
        this.index--;
        String literal = this.input.substring(startIndex, this.index);
        return createToken(TokenType.TOKEN_INT_LITERAL, literal);

    }
    private Token parseBinary(int startIndex){
        char currentChar = advance();

        while(!isOutOfBounds() && (currentChar == '0' || currentChar == '1')){
            currentChar = advance();
        }
        this.index--;
        String literal = this.input.substring(startIndex, this.index);
        return createToken(TokenType.TOKEN_INT_LITERAL, literal);
    }
    private Token parseNumber(){
        int startIndex = this.index - 1;
        TokenType type = TokenType.TOKEN_INT_LITERAL;
        char currentChar = advance();
        if(this.input.charAt(startIndex) == '0'){
            if(currentChar == 'x'){
                return this.parseHex(startIndex);
            }
            else if(currentChar == 'b'){
                return this.parseBinary(startIndex);
            }
        }

        while(!isOutOfBounds() && Character.isDigit(currentChar)){
            currentChar = advance();
        }
        if(currentChar == '.'){
            currentChar = getCurrentChar();
            type = TokenType.TOKEN_FLOAT_LITERAL;
            while(!isOutOfBounds() && Character.isDigit(currentChar)){
                currentChar = advance();
            }
        }
        this.index--;
        String literal = this.input.substring(startIndex, this.index);
        return createToken(type, literal);

    }

    private Token createToken(TokenType tokenType, String literal){
        return new Token(tokenType, this.line, literal);
    }

    private boolean matchNext(char toMatch){
        if(getCurrentChar() == toMatch){
            this.index++;
            return true;
        }
        return false;
    }

    private char advance(){
        char out = getCurrentChar();
        this.index++;
        return out;
    }

    private boolean isOutOfBounds(){
        return this.index >= this.input.length();
    }

    public Token parseToken() throws CompileException{
        skipWhiteSpace();

        if(this.isOutOfBounds()){
            return this.createToken(TokenType.TOKEN_EOF, "EOF");
        }

        char currentChar = advance();
        if(Character.isAlphabetic(currentChar)){
            return parseKeyword();
        }
        if(Character.isDigit(currentChar)){
            return parseNumber();
        }

        return switch (currentChar) {
            case '{' -> this.createToken(TokenType.TOKEN_LEFT_BRACE, "{");
            case '}' -> this.createToken(TokenType.TOKEN_RIGHT_BRACE, "}");
            case '/' ->{
                if(matchNext('=')){
                    yield this.createToken(TokenType.TOKEN_AUGMENTED_SLASH, "/=");
                }
                yield this.createToken(TokenType.TOKEN_SLASH, "/");
            }
            case '*' -> {
                if(matchNext('=')){
                    yield this.createToken(TokenType.TOKEN_AUGMENTED_STAR, "*=");
                }
                yield this.createToken(TokenType.TOKEN_STAR, "*");
            }
            case '[' -> this.createToken(TokenType.TOKEN_LEFT_BRACKET, "[");
            case ']' -> this.createToken(TokenType.TOKEN_RIGHT_BRACKET, "]");
            case ';' -> this.createToken(TokenType.TOKEN_SEMICOLON, ";");
            case '%' -> this.createToken(TokenType.TOKEN_MOD, "%");
            case ',' -> this.createToken(TokenType.TOKEN_COMMA, ",");
            case '#' ->
            {
                if(this.input.startsWith("include", this.index)){
                    this.index += "include".length();
                    yield this.createToken(TokenType.TOKEN_INCLUDE, "#include");
                }
                if(this.input.startsWith("extern", this.index)){
                    this.index += "extern".length();
                    yield this.createToken(TokenType.TOKEN_EXTERN, "#extern");
                }
                if(this.input.startsWith("define", this.index)){
                    this.index += "define".length();
                    yield this.createToken(TokenType.TOKEN_DEFINE, "#define");
                }
                throw new CompileException(String.format("tried to parse #smth but failed at line %d\n", line));
            }
            case '.' ->{
                if(this.input.startsWith("..", this.index)){
                    this.index += "..".length();
                    yield this.createToken(TokenType.TOKEN_ELLIPSIS, "...");
                }
                yield this.createToken(TokenType.TOKEN_DOT, ".");
            }
            case '(' -> this.createToken(TokenType.TOKEN_LEFT_PAREN, "(");
            case ')' -> this.createToken(TokenType.TOKEN_RIGHT_PAREN, ")");
            case '^' -> {
                if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_AUGMENTED_XOR, "^=");
                }
                yield this.createToken(TokenType.TOKEN_XOR, "^");
            }
            case '&' -> {
                if (matchNext('&')) {
                    yield this.createToken(TokenType.TOKEN_AND_LOGICAL, "&&");
                }
                if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_AUGMENTED_AND, "&=");
                }
                yield this.createToken(TokenType.TOKEN_AND_BIT, "&");
            }
            case '|' -> {
                if (matchNext('|')) {
                    yield this.createToken(TokenType.TOKEN_OR_LOGICAL, "||");
                }
                if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_AUGMENTED_OR, "|=");
                }
                yield this.createToken(TokenType.TOKEN_OR_BIT, "|");
            }
            case '!' -> {
                if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_BANG_EQUAL, "!=");
                }
                yield this.createToken(TokenType.TOKEN_BANG, "!");
            }
            case '>' -> {
                if (matchNext('>')) {
                    yield this.createToken(TokenType.TOKEN_SHIFT_RIGHT, ">>");
                }
                if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_GREATER_EQUAL, ">=");
                }
                yield this.createToken(TokenType.TOKEN_GREATER, ">");
            }
            case '<' -> {
                if (matchNext('<')) {
                    yield this.createToken(TokenType.TOKEN_SHIFT_LEFT, "<<");
                }
                if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_LESS_EQUAL, "<=");
                }
                yield this.createToken(TokenType.TOKEN_LESS, "<");
            }
            case '-' -> {
                if (matchNext('-')) {
                    yield this.createToken(TokenType.TOKEN_DECREMENT, "--");
                } else if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_AUGMENTED_MINUS, "+=");
                }
                yield this.createToken(TokenType.TOKEN_MINUS, "-");
            }
            case '+' -> {
                if (matchNext('+')) {
                    yield this.createToken(TokenType.TOKEN_INCREMENT, "++");
                } else if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_AUGMENTED_PLUS, "+=");
                }
                yield this.createToken(TokenType.TOKEN_PLUS, "+");
            }
            case '=' -> {
                if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_EQUAL_EQUAL, "==");
                }
                yield this.createToken(TokenType.TOKEN_EQUAL, "=");
            }
            case '\"' -> parseString();
            default ->
                    throw new CompileException(String.format("Illegal character '%c' at line %d\n", currentChar, line));
        };
    }


    public Scanner(String input){
        this.input = input;
        this.index = 0;
        this.line = 1;
    }
}
