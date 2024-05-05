package se.liu.albhe576.project.frontend;

import java.util.Map;
import se.liu.albhe576.project.backend.Compiler;

/**
 * A Scanner reads through the given input stream and creates Tokens and supplies them to the parser.
 *
 * @see Token
 * @see TokenType
 *
 */
public class Scanner {
    private final String input;
    private int index;
    private int line;
    private final String filename;

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
        // skip until we hit something we care about
	while (true) {
	    char currentChar = this.getCurrentChar();
	    if(currentChar == 0 || Character.isDigit(currentChar) || Character.isAlphabetic(currentChar)){
		return;
	    }
	    switch(currentChar){
		case '/' -> {
		    advance();
		    if(this.matchNext('/')){
			while(!this.isOutOfBounds() && this.getCurrentChar() != '\n'){
			    advance();
			}
			line++;
			break;
		    }
		    this.index--;
		    return;
		}
		case '\"', '{', '[',
		']', '}', '*', ';',
		'<', '>', '(', ')',
		'!', '%', '.', '#',
		'^', ',', '&', '|',
		'-', '+', '=' ->{
		    return;
		}
		case '\n'->{
		    this.line++;
		}
		default -> {
		    // Just continue skipping
		}
	    }
	    index++;
	}
    }

    private Token parseString() {
        int startIndex  = this.index;
        int line        = this.line;

        // parse until we hit the next '"'
        char currentChar = advance();
        while(!isOutOfBounds() && currentChar != '\"'){
            currentChar = advance();
        }

        // check for unterminated strings
        if(isOutOfBounds()){
            Compiler.panic("Unterminated string at starting", line, filename);
        }

        String literal = this.input.substring(startIndex, this.index - 1);
        return createToken(TokenType.TOKEN_STRING_LITERAL, literal);
    }
    private final static Map<String, TokenType> RESERVED_KEYWORDS = Map.ofEntries(
            Map.entry("struct", TokenType.TOKEN_STRUCT),
            Map.entry("string", TokenType.TOKEN_STRING),
            Map.entry("if", TokenType.TOKEN_IF),
            Map.entry("else", TokenType.TOKEN_ELSE),
            Map.entry("for", TokenType.TOKEN_FOR),
            Map.entry("while", TokenType.TOKEN_WHILE),
            Map.entry("return", TokenType.TOKEN_RETURN),
            Map.entry("short", TokenType.TOKEN_SHORT),
            Map.entry("int", TokenType.TOKEN_INT),
            Map.entry("long", TokenType.TOKEN_LONG),
            Map.entry("double", TokenType.TOKEN_DOUBLE),
            Map.entry("byte", TokenType.TOKEN_BYTE),
            Map.entry("float", TokenType.TOKEN_FLOAT),
            Map.entry("void", TokenType.TOKEN_VOID)
    );
    public TokenType getKeyword(String literal){
        if(RESERVED_KEYWORDS.containsKey(literal)){
            return RESERVED_KEYWORDS.get(literal);
        }
        return TokenType.TOKEN_IDENTIFIER;
    }

    private Token parseKeyword() {
        int startIndex = this.index - 1;
        char currentChar = advance();
        // keep parsing while we're not out of boudns and we have a digit, character or _
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

    private boolean continueParsingNumber(char current){
        return !isOutOfBounds() && Character.isDigit(current);
    }
    private Token parseNumber(){
        int startIndex = this.index - 1;
        TokenType type = TokenType.TOKEN_INT_LITERAL;

        char currentChar = advance();
        // check for binary and hex values
        if(this.input.charAt(startIndex) == '0'){
            if(currentChar == 'x'){
                return this.parseHex(startIndex);
            }
            else if(currentChar == 'b'){
                return this.parseBinary(startIndex);
            }
        }

        while(continueParsingNumber(currentChar)){
            currentChar = advance();
        }

        // check for float
        if(currentChar == '.'){
            currentChar = getCurrentChar();
            type = TokenType.TOKEN_FLOAT_LITERAL;
            while(continueParsingNumber(currentChar)){
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

    public Token parseToken() {
        skipWhiteSpace();

        if(this.isOutOfBounds()){
            return this.createToken(TokenType.TOKEN_EOF, "EOF");
        }

        char currentChar = advance();
        // check number and identifier
        if(Character.isAlphabetic(currentChar)){
            return parseKeyword();
        }
        if(Character.isDigit(currentChar)){
            return parseNumber();
        }

        return switch (currentChar) {
            // check single character tokens
            case '{' -> this.createToken(TokenType.TOKEN_LEFT_BRACE, "{");
            case '}' -> this.createToken(TokenType.TOKEN_RIGHT_BRACE, "}");
            case '[' -> this.createToken(TokenType.TOKEN_LEFT_BRACKET, "[");
            case ']' -> this.createToken(TokenType.TOKEN_RIGHT_BRACKET, "]");
            case ';' -> this.createToken(TokenType.TOKEN_SEMICOLON, ";");
            case '%' -> this.createToken(TokenType.TOKEN_MOD, "%");
            case ',' -> this.createToken(TokenType.TOKEN_COMMA, ",");
            case '(' -> this.createToken(TokenType.TOKEN_LEFT_PAREN, "(");
            case ')' -> this.createToken(TokenType.TOKEN_RIGHT_PAREN, ")");
            case '\"' -> parseString();
            // parse pound defines
            case '#' ->
            {
                if(this.input.startsWith("include", this.index)){
                    this.index += "include".length();
                    yield this.createToken(TokenType.TOKEN_INCLUDE, "#include");
                }
                else if(this.input.startsWith("extern", this.index)){
                    this.index += "extern".length();
                    yield this.createToken(TokenType.TOKEN_EXTERN, "#extern");
                }
                else if(this.input.startsWith("define", this.index)){
                    this.index += "define".length();
                    yield this.createToken(TokenType.TOKEN_DEFINE, "#define");
                }
                Compiler.panic("tried to parse '#' but didnt find include/exctern/define afterwards\n", line, filename);
                // unreachable
                yield null;
            }
            // check two character tokens
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
            case '.' ->{
                if(this.input.startsWith("..", this.index)){
                    this.index += "..".length();
                    yield this.createToken(TokenType.TOKEN_ELLIPSIS, "...");
                }
                yield this.createToken(TokenType.TOKEN_DOT, ".");
            }
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
                else if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_AUGMENTED_AND, "&=");
                }
                yield this.createToken(TokenType.TOKEN_AND_BIT, "&");
            }
            case '|' -> {
                if (matchNext('|')) {
                    yield this.createToken(TokenType.TOKEN_OR_LOGICAL, "||");
                }
                else if (matchNext('=')) {
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
                else if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_GREATER_EQUAL, ">=");
                }
                yield this.createToken(TokenType.TOKEN_GREATER, ">");
            }
            case '<' -> {
                if (matchNext('<')) {
                    yield this.createToken(TokenType.TOKEN_SHIFT_LEFT, "<<");
                }
                else if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_LESS_EQUAL, "<=");
                }
                yield this.createToken(TokenType.TOKEN_LESS, "<");
            }
            case '-' -> {
                if (matchNext('-')) {
                    yield this.createToken(TokenType.TOKEN_DECREMENT, "--");
                } else if (matchNext('=')) {
                    yield this.createToken(TokenType.TOKEN_AUGMENTED_MINUS, "+=");
                }else if(Character.isDigit(getCurrentChar())){
                   yield this.parseNumber();
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
            default ->{
                Compiler.panic(String.format("Illegal character '%c'", currentChar), line, filename);
                // Unreachable
                yield null;
            }
        };
    }
    public Scanner(String input, String filename){
        this.filename = filename;
        this.input = input;
        this.index = 0;
        this.line = 1;
    }
}
