package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.ArrayDataType;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;
import se.liu.albhe576.project.backend.DataType;
import se.liu.albhe576.project.backend.Function;
import se.liu.albhe576.project.backend.SymbolTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * The parser for the language.
 * It consumes tokens given by the scanner and transform them into an abstract syntax tree.
 * It uses advance, matchType and consume as it primary functions to get a new token and continue parsing
 * <p>
 * The parser supports single line macros (without arguments) which just hijacks the token stream if it finds an identifier that's been defined and inserts the tokens in the macro
 * It also supports #include statements which tries to open a given filename and parses that before continuing with it's current file.
 * <p>
 * It's a pratt parser which defines a precedence level, functions for prefix and postfix for a TokenType in order to solve the problem of precedence.
 * <p>
 * The pre and post fix functions are defined to determine what type of expression to be parsed when there is ambiguity.
 * So that when for an example we encounter a "-", which can be either a unary expression (prefix) such as "2 * (-5)" or a binary expression like "2 - 5" (postfix)
 * <p>
 * In order to solve precedence we define a precedence level for each token. So to solve that 1 + 2 * 3 -> 1 + (2 * 3) it will look something like this
 * We first encounter the token for "1" which has no prefix rule and we continue
 * Then we get a "+", we parse the postfix rule which is a binary expression
 * When parsing a binary expression we then increase the precedence level from term ('+'s precedence) to the level above (factor in this case)
 * We do this so that if we encounter another "+" the precedence is lower then our current one and we have finished parsing the expression
 * In this case the next token is a "*" which has equal (could be higher) precedence which we start parsing a NEW expression which will be the right side of "1 +" in this case
 * After we've parsed the right side we then return we then get 1 + (2 * 3)
 * <p>
 * For a explanation from someone whose not a banana see:
 *  <a href="https://tdop.github.io/">...</a>
 *  <a href="https://matklad.github.io/2020/04/13/simple-but-powerful-pratt-parsing.html">...</a>
 *
 * @see Precedence
 * @see Scanner
 * @see Compiler
 * @see Expression
 * @see Statement
 * @see Token
 */

public class Parser {
    @FunctionalInterface
    private interface Rule{
        Expression apply(Expression expression, boolean canAssign, int line) throws CompileException;
    }
    private record ParseFunction(Rule prefixRule, Rule infixRule, Precedence precedence) { }
    private final EnumMap<TokenType, ParseFunction> parseFunctions = new EnumMap<>(Map.ofEntries(
        Map.entry(TokenType.TOKEN_LEFT_BRACKET, new ParseFunction(null, this::parseIndex, Precedence.CALL)),
        Map.entry(TokenType.TOKEN_LEFT_PAREN, new ParseFunction(this::parseGroupedExpression, null, Precedence.CALL)),
        Map.entry(TokenType.TOKEN_DOT, new ParseFunction(null, this::parseDotExpression, Precedence.CALL)),
        Map.entry(TokenType.TOKEN_MINUS, new ParseFunction(this::parseUnary, this::parseBinary, Precedence.TERM)),
        Map.entry(TokenType.TOKEN_PLUS, new ParseFunction(null, this::parseBinary, Precedence.TERM)),
        Map.entry(TokenType.TOKEN_SLASH, new ParseFunction(null, this::parseBinary, Precedence.FACTOR)),
        Map.entry(TokenType.TOKEN_STAR, new ParseFunction(this::parseDereference, this::parseBinary, Precedence.FACTOR)),
        Map.entry(TokenType.TOKEN_BANG, new ParseFunction(this::parseUnary, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_BANG_EQUAL, new ParseFunction(null, this::parseComparison, Precedence.EQUALITY)),
        Map.entry(TokenType.TOKEN_EQUAL_EQUAL, new ParseFunction(null, this::parseComparison, Precedence.EQUALITY)),
        Map.entry(TokenType.TOKEN_GREATER, new ParseFunction(null, this::parseComparison, Precedence.COMPARISON)),
        Map.entry(TokenType.TOKEN_GREATER_EQUAL, new ParseFunction(null, this::parseComparison, Precedence.COMPARISON)),
        Map.entry(TokenType.TOKEN_LESS, new ParseFunction(null, this::parseComparison, Precedence.COMPARISON)),
        Map.entry(TokenType.TOKEN_LESS_EQUAL, new ParseFunction(null, this::parseComparison, Precedence.COMPARISON)),
        Map.entry(TokenType.TOKEN_IDENTIFIER, new ParseFunction(this::parseVariable, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_STRING_LITERAL, new ParseFunction(this::parseLiteral, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_INT_LITERAL, new ParseFunction(this::parseLiteral, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_FLOAT_LITERAL, new ParseFunction(this::parseLiteral, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_AND_LOGICAL, new ParseFunction(null, this::parseLogical, Precedence.AND)),
        Map.entry(TokenType.TOKEN_AND_BIT, new ParseFunction(this::parseUnary, this::parseBinary, Precedence.BITWISE)),
        Map.entry(TokenType.TOKEN_OR_LOGICAL, new ParseFunction(null, this::parseLogical, Precedence.OR)),
        Map.entry(TokenType.TOKEN_OR_BIT, new ParseFunction(null, this::parseBinary, Precedence.BITWISE)),
        Map.entry(TokenType.TOKEN_XOR, new ParseFunction(null, this::parseBinary, Precedence.BITWISE)),
        Map.entry(TokenType.TOKEN_SHIFT_LEFT, new ParseFunction(null, this::parseBinary, Precedence.BITWISE)),
        Map.entry(TokenType.TOKEN_SHIFT_RIGHT, new ParseFunction(null, this::parseBinary, Precedence.BITWISE)),
        Map.entry(TokenType.TOKEN_INCREMENT, new ParseFunction(this::parseUnary, this::parsePostfix, Precedence.TERM)),
        Map.entry(TokenType.TOKEN_DECREMENT, new ParseFunction(this::parseUnary, this::parsePostfix, Precedence.TERM)),
        Map.entry(TokenType.TOKEN_MOD, new ParseFunction(null, this::parseBinary, Precedence.TERM))
    ));
    private final Map<String, Deque<Token>> defined;
    private final List<String> included;
    private final String fileName;
    private final Map<String, Function> functions;
    private final Map<String, Structure> structures;
    private final Scanner scanner;
    private final Logger logger;
    private final FileHandler fileHandler;
    private Token current;
    private Token previous;
    private int tokenCount;
    private final Deque<Token> definedQueue;
    public Map<String, Function> getFunctions(){return this.functions;}
    public Map<String, Deque<Token>> getDefined(){return this.defined;}
    public Map<String, Structure> getStructures(){return this.structures;}

    private void updateCurrent(){
        if(this.current.type() == TokenType.TOKEN_IDENTIFIER && this.defined.containsKey(this.current.literal())){
            for(Token token : this.defined.get(this.current.literal())){
                this.definedQueue.addFirst(token);
            }
            this.current = this.definedQueue.removeLast();
        }
    }
    private void parseSizeOf() throws CompileException {
        advance();
        if(this.previous.type() != TokenType.TOKEN_LEFT_PAREN){
            Compiler.panic("Expected '(' after sizeof", this.scanner.getLine(), this.fileName);
        }
        advance();
        Token type = this.previous;
        advance();
        if(this.previous.type() != TokenType.TOKEN_RIGHT_PAREN){
            Compiler.panic("Expected ')' after sizeof", this.scanner.getLine(), this.fileName);
        }
        int size = SymbolTable.getStructureSize(this.structures, DataType.getDataTypeFromToken(type));
        this.previous = new Token(TokenType.TOKEN_INT_LITERAL, String.valueOf(size));
    }

    private void advance() throws CompileException{
        previous = current;
        if(!this.definedQueue.isEmpty()){
            current = this.definedQueue.pop();
        }else{
            current = scanner.parseToken();
            System.out.println(current.type());
            this.tokenCount++;
        }

        this.updateCurrent();

        if(this.previous != null && this.previous.type() == TokenType.TOKEN_IDENTIFIER && this.previous.literal().equals("sizeof")){
            this.parseSizeOf();
        }
    }

    private boolean matchType(TokenType type) throws CompileException{
        if(this.current.type() != type){
           return false;
        }
        advance();
        return true;
    }

    private boolean isVariableType(TokenType currentType) {
        return (currentType == TokenType.TOKEN_INT ||
                currentType == TokenType.TOKEN_FLOAT ||
                currentType == TokenType.TOKEN_STRING ||
                currentType == TokenType.TOKEN_BYTE ||
                currentType == TokenType.TOKEN_LONG ||
                currentType == TokenType.TOKEN_SHORT ||
                currentType == TokenType.TOKEN_DOUBLE ||
                currentType == TokenType.TOKEN_VOID
        );
    }

    private DataType parsePointerType(DataType type) throws CompileException{
        while(matchType(TokenType.TOKEN_STAR)){
            type = type.getPointerFromType();
        }
        return type;
    }
    private void panic(String msg, int line){
        Compiler.panic(msg, line, this.fileName);
    }
    private void panic(String msg){
        Compiler.panic(msg, this.scanner.getLine(), this.fileName);
    }

    private DataType parseType() throws CompileException{
        DataType dataType = DataType.getDataTypeFromToken(this.current);
        if(dataType.isStructure() && !this.structures.containsKey(dataType.getName())){
            panic(String.format("Can't declare struct field of non existing type '%s'", dataType.getName()));
        }
        advance();
        return this.parsePointerType(dataType);
    }

    private StructureField parseStructField() throws CompileException{
        DataType dataType = this.parseType();
        if(!matchType(TokenType.TOKEN_IDENTIFIER)){
            Compiler.panic(String.format("Expected identifier as argument but got %s", this.current.type()), this.scanner.getLine(), this.fileName);
        }
        return new StructureField(this.previous.literal(), dataType);
    }

    private Expression parsePostfix(Expression expression, boolean canAssign, int line){return new PostfixExpression(expression, this.previous, this.scanner.getLine(), this.fileName);}

    private void parseStructDeclaration() throws CompileException {
        // a struct declaration is
        // struct foo{
        //      type name;
        //      type name;
        //  } // the last ; as in C isn't neccessary here
        TokenType currentType   = this.current.type();
        int currentLine         = this.scanner.getLine();

        // parse the struct name
        if(!matchType(TokenType.TOKEN_IDENTIFIER)){
            Compiler.panic(String.format("Expected identifier but got %s", currentType), currentLine, this.fileName);
        }
        Token name = this.previous;
        // check if the struct is already declared
        if(this.structures.containsKey(name.literal())){
            Structure structure = this.structures.get(name.literal());
            Compiler.panic(String.format("Can't declare struct '%s' which is already declared from file %s", name.literal(), structure.getFilename()), this.scanner.getLine(), this.fileName);
        }

        if(!matchType(TokenType.TOKEN_LEFT_BRACE)){
            Compiler.panic(String.format("Expected left brace but got %s", currentType), currentLine, this.fileName);
        }

        // parse the fields
        List<StructureField> fields = new ArrayList<>();
        while(!matchType(TokenType.TOKEN_RIGHT_BRACE)){
            fields.add(this.parseStructField());
            if(!matchType(TokenType.TOKEN_SEMICOLON)){
                panic(String.format("Expected semicolon after struct field but got %s", this.current.type()));
            }
        }

        this.structures.put(name.literal(), new Structure(fields, this.fileName));
    }

    private void consume(TokenType type, String msg) throws CompileException{
        if(!matchType(type)){
            panic(msg);
        }
    }
    private Expression parseLiteral(Expression expression, boolean canAssign, int line) {
        return new LiteralExpression(this.previous, line, this.fileName);
    }

    private ParseFunction getEmptyParseFunction(){
        return new ParseFunction(null, null, Precedence.NONE);
    }
    private ParseFunction getParseFunction(TokenType type){
        return this.parseFunctions.getOrDefault(type, getEmptyParseFunction());
    }
    private Expression parseExpr(Expression expression, Precedence precedence) throws CompileException{

        // this is the main function for parsing expressions
        // this should be called when starting to parse an expression
        int line = this.scanner.getLine();
        advance();

        // look for a prefix rule
        ParseFunction prefix = this.getParseFunction(this.previous.type());
        if(prefix.prefixRule == null){
            panic(String.format("Expected expression but got %s %s", this.previous.literal(), precedence));
        }

        final int precedenceOrdinal = precedence.ordinal();
        final boolean canAssign =  precedenceOrdinal <= Precedence.ASSIGNMENT.ordinal();

        // parse the prefix
        expression                  = prefix.prefixRule.apply(expression, canAssign, line);
        ParseFunction currentRule   = getParseFunction(this.current.type());

        // as long as the current rule has bigger or the same precedence as the current rule
        // we continue to call the infix rule
        while(precedenceOrdinal <= currentRule.precedence.ordinal()){
            advance();
            expression = currentRule.infixRule.apply(expression, canAssign, line);
            currentRule = getParseFunction(this.current.type());
        }

        if(canAssign && matchType(TokenType.TOKEN_EQUAL)){
            Compiler.panic("Can't assign to this", this.scanner.getLine(), this.fileName);
        }
        return expression;
    }

    private boolean matchAugmented() throws CompileException{
        // check if we match any augmented expressions
        // which is +=, -= etc
        return (
                matchType(TokenType.TOKEN_AUGMENTED_XOR) ||
                matchType(TokenType.TOKEN_AUGMENTED_AND)  ||
                matchType(TokenType.TOKEN_AUGMENTED_OR) ||
                matchType(TokenType.TOKEN_AUGMENTED_MINUS) ||
                matchType(TokenType.TOKEN_AUGMENTED_STAR)  ||
                matchType(TokenType.TOKEN_AUGMENTED_SLASH) ||
                matchType(TokenType.TOKEN_AUGMENTED_PLUS)
        );
    }

    private Expression parseEmptyExpression() throws CompileException {return parseExpr(null, Precedence.ASSIGNMENT);}

    private Statement parseExpressionStatement() throws CompileException {
        int line = this.scanner.getLine();

        // parse the expression
        Expression expression = parseExpr(null, Precedence.OR);


        // check if we assign or not
        if(matchType(TokenType.TOKEN_EQUAL)) {
            Expression rightSide = this.parseEmptyExpression();
            return new AssignStatement(expression, rightSide, line, this.fileName);
        }
        // check if we it's an augmented expression
        else if(matchAugmented()){
            Token op        = this.previous;
            Expression rightSide  = this.parseEmptyExpression();
            return new AssignStatement(expression, new BinaryExpression(expression, op, rightSide, line, this.fileName), line, this.fileName);
        }
        return new ExpressionStatement(expression, line, this.fileName);
    }

    private Statement parseReturnStatement() throws CompileException{
        int line = this.scanner.getLine();
        this.advance();
        // Check if it's an empty "return;"
        if(matchType(TokenType.TOKEN_SEMICOLON)){
            return new ReturnStatement(null, line, this.fileName);
        }
        // otherwise we parse the statement
        Statement out = new ReturnStatement(this.parseEmptyExpression(), line, this.fileName);
        consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after return stmt %s", out));
        return out;
    }
    private Statement getEmptyExpressionStatement(int line){
        return new ExpressionStatement(null, line, this.fileName);
    }
    private Statement parseForStatement() throws CompileException {
        int line = this.scanner.getLine();
        this.advance();
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected '(' after for");

        // parse the initializer, condition and update statements
        Statement init = parseStatement();

        Statement condition;
        if(!matchType(TokenType.TOKEN_SEMICOLON)){
            condition = parseExpressionStatement();
            consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after condition stmt %s", condition));
        }else{
            condition = getEmptyExpressionStatement(line);
        }

        Statement update;
        if(!matchType(TokenType.TOKEN_RIGHT_PAREN)){
            update = parseExpressionStatement();
            consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after loop control variables");
        }else{
            update = getEmptyExpressionStatement(line);
        }
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' in for loop");

        // parse the body block
        List<Statement> body = this.parseBody();
        return new ForStatement(init, condition, update, body, line, this.fileName);

    }
    private Statement parseWhileStatement() throws CompileException {
        int line = this.scanner.getLine();
        this.advance();

        // match (condition){
        consume(TokenType.TOKEN_LEFT_PAREN, "expected '(' after while");
        Expression condition = this.parseEmptyExpression();
        consume(TokenType.TOKEN_RIGHT_PAREN, "expected ')' after while");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after while condition");

        // then just parse the body block
        List<Statement> body = parseBody();
        return new WhileStatement(condition, body, line, this.fileName);
    }
    private List<Statement> parseBody() throws CompileException {
        List<Statement> body = new ArrayList<>();
        // continue parsing while we don't hit a end of block i.e '}'
        while(!matchType(TokenType.TOKEN_RIGHT_BRACE)) {
            // check for empty ;
            if(!matchType(TokenType.TOKEN_SEMICOLON)){
                body.add(parseStatement());
            }
        }
        return body;
    }
    private List<StructureField> parseArguments() throws CompileException {

        // parse function arguments, which means we parse until we hit ')' (or fails to parse a ',')
	List<StructureField> args = new ArrayList<>();
        // check for no argument
        if(this.current.type() != TokenType.TOKEN_RIGHT_PAREN){
            // otherwise parse while we hit a ','
	    do{
                StructureField field = this.parseStructField();
                if(args.stream().anyMatch(x -> x.name().equals(field.name()))){
                    Compiler.panic(String.format("Can't declare multiple fields/arguments with the same name '%s'!", field.name()), this.scanner.getLine(), this.fileName);
                }
                args.add(field);

            }while(matchType(TokenType.TOKEN_COMMA));
        }
        // consume the last ')'
        consume(TokenType.TOKEN_RIGHT_PAREN, String.format("Expected right paren after arguments but got %s", this.current.type()));
        return args;
    }

    private Statement parseArray(String name, DataType type, int line) throws CompileException {

        // an array declaration is int foo[n] = []
        // so parse the amount of items (n)
        ArrayDataType arrayType = ArrayDataType.fromItemType(type);
        consume(TokenType.TOKEN_INT_LITERAL, String.format("Expected array size in form of int literal in array declaration, got %s", this.current.literal()));

        // since an integer literal can be binary/hex we use decode rather then parseInt
        // so you can do int foo[0xF]
        int size = Integer.decode(this.previous.literal());
        consume(TokenType.TOKEN_RIGHT_BRACKET, String.format("Expected ']' after array size in array declaration, got %s", this.current.literal()));

        // parse the array items, note that they don't have to align with the size declared
        // we check this in ArrayStatement.compile
        List<Expression> items = new ArrayList<>();
        if(matchType(TokenType.TOKEN_EQUAL)){
            consume(TokenType.TOKEN_LEFT_BRACKET, "Expected '[' after = in array declaration");
            items = parseArrayItems(size);
        }

        consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after array declaration, not %s?", this.current.literal()));
        return new ArrayStatement(arrayType, name, items, size, line, fileName);

    }

    private Statement parseVariableDeclaration(DataType type) throws CompileException {
        int line = this.scanner.getLine();

        // so we have parsed the type of the variable and now parse its name
        consume(TokenType.TOKEN_IDENTIFIER, String.format("Expected identifier after variable type but got %s", this.current.type()));
        String name = this.previous.literal();

        // check whether or not its a just a declaration without assignment i.e int a; or it has an assignment as well
        // or if it's an array
        advance();
        switch(this.previous.type()){
            case TOKEN_EQUAL->{
                Expression value = parseExpr(null, Precedence.ASSIGNMENT);
                consume(TokenType.TOKEN_SEMICOLON, String.format("expected semicolon after assign expr but got %s", this.current.type()));
                return new VariableStatement(type, name, value, line, this.fileName);
            }
            case TOKEN_SEMICOLON -> {return new VariableStatement(type, name, null, line, this.fileName);}
            default -> {
                if(this.previous.type() != TokenType.TOKEN_LEFT_BRACKET){
                    panic("Expected '[', '=', '(' or ';' after variable name");
                }
                return this.parseArray(name, type, line);
            }
        }
    }
    private IfBlock parseIfBlock() throws CompileException {
        // parse (condition){ and the body
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected '(' after if");
        Expression condition = this.parseEmptyExpression();

        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after if condition");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after if condition");

        return new IfBlock(condition, parseBody());
    }
    private Statement parseIfStatement() throws CompileException {
        this.advance();
        int line                        = this.scanner.getLine();
        List<IfBlock> ifBlocks          = new ArrayList<>();
        List<Statement> elseBody        = new ArrayList<>();
        // parse the first ifBlock
        ifBlocks.add(this.parseIfBlock());

        // then keep parsing else if if present
        while(matchType(TokenType.TOKEN_ELSE)){
            if(matchType(TokenType.TOKEN_IF)){
               ifBlocks.add(this.parseIfBlock());
            }else{
                consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after if condition");
                elseBody = parseBody();
            }
        }

        return new IfStatement(ifBlocks, elseBody, line, this.fileName);
    }
    private Statement parseStatement() throws CompileException {
        switch(this.current.type()){
            case TOKEN_FOR      -> {return parseForStatement();}
            case TOKEN_WHILE    -> {return parseWhileStatement();}
            case TOKEN_IF       -> {return parseIfStatement();}
            case TOKEN_RETURN   -> {return parseReturnStatement();}
            default -> {
                // parse a variable declaration
                if(isVariableType(this.current.type()) || this.structures.containsKey(this.current.literal())){
                    DataType type = parseType();
                    return parseVariableDeclaration(type);
                }
                // or just an empty extra ;
                else if(matchType(TokenType.TOKEN_SEMICOLON)){
                    // note we return here since we have to :)
                    return new ExpressionStatement(null, this.scanner.getLine(), this.fileName);
                }
                Statement out = parseExpressionStatement();
                consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after expression Stmt %s", out));
                return out;
            }

        }
    }
    private void parseInclude() throws CompileException {

        // an include is just #include "filename.jc"
        consume(TokenType.TOKEN_STRING_LITERAL, "Expected string after include?");
        String fileName = this.previous.literal();

        // check if we've already included this file or not
        if(this.included.stream().anyMatch(i -> i.equals(fileName))){
            return;
        }


        // otherwise add it
        this.included.add(fileName);
        // try to parse it or panic if we can't
        String s = null;
        try{
            s = Files.readString(Path.of(fileName));
        }catch(IOException ignored){
            logger.severe(String.format("Failed to read from include '%s' included at %s:%d", fileName, this.fileName,this.scanner.getLine()));
            panic(String.format("Failed to read from include %s", fileName));
        }

        // otherwise try to parse the file
        Parser includeParser = new Parser(new Scanner(s, fileName), this.included, this.structures, fileName, this.fileHandler);
        includeParser.parse();

        // grab all the things defined within the file
        this.functions.putAll(includeParser.getFunctions());
        this.defined.putAll(includeParser.getDefined());
        this.structures.putAll(includeParser.structures);
    }

    private void parseDefine() throws CompileException {

        // defines a macro #define name expr
        int line = this.scanner.getLine();
        consume(TokenType.TOKEN_IDENTIFIER, "Expected name of macro!");

        // We define macros as a sequence of tokens defined on the same line
        // so keep parsing if possible until we hit the next line
        String name = this.previous.literal();
        Deque<Token> macro = new ArrayDeque<>();

        while(this.current.type() != TokenType.TOKEN_EOF  && this.scanner.getLine() == line){
            macro.addLast(this.current);
            advance();
        }

        // add the macro
        if(macro.isEmpty()){
            Compiler.panic("Can't declare an empty macro!", line, this.fileName);
        }
        this.defined.put(name, macro);
    }
    private void parseExtern() throws CompileException {
        // check if we've already declared this
        if(!(isVariableType(this.current.type()) || this.structures.containsKey(this.current.literal()))) {
            Compiler.panic(String.format("Expected returntype after #extern, got %s", this.current.literal()), this.scanner.getLine(), this.fileName);
        }
        DataType type = this.parseType();

        // otherwise parse the name
        consume(TokenType.TOKEN_IDENTIFIER, "Expected identifier for function name after #extern");
        String funcName = this.previous.literal();

        // and parse its arguments, we allow varargs (variable arguments?) for external functions
        // in order to support calling things like printf
        int line = this.scanner.getLine();
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected ( for function args after function name in extern");
        if(matchType(TokenType.TOKEN_ELLIPSIS)){
            this.functions.put(funcName, new Function(null, type, null, this.fileName, line, true));
            consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ) after function args in extern");
        }else{
            // if there isn't varargs try to parse it's arguments
            List<StructureField> arguments = this.parseArguments();
            this.functions.put(funcName, new Function(arguments, type, null, this.fileName, line, false));
        }
    }
    private void parseFunction() throws CompileException {
        if(!(isVariableType(this.current.type()) || this.structures.containsKey(this.current.literal()))){
            Compiler.panic("Can't declare something other then extern, include, struct or function in outer scope", this.scanner.getLine(), this.fileName);
        }

        // parse its return type and name
        DataType type = parseType();
        int line = this.scanner.getLine();
        consume(TokenType.TOKEN_IDENTIFIER, String.format("Expected function name after type but got %s", this.current.type()));
        String name = this.previous.literal();

        // check if its already declared
        if(this.functions.containsKey(name) || this.structures.containsKey(name)){
            Function existingFunction = this.functions.get(name);
            Compiler.panic(String.format("Can't redeclare function '%s', already declare at %s:%d", name, existingFunction.getFile(),existingFunction.getLine()), this.scanner.getLine(), this.fileName);
        }

        // parse the arguments
        consume(TokenType.TOKEN_LEFT_PAREN, String.format("Expected '(' for function parameters when parsing function signature in outer scope, got %s", this.current.literal()));
        List<StructureField> args = this.parseArguments();

        consume(TokenType.TOKEN_LEFT_BRACE, String.format("Expected { to start function body but got %s", this.current.type()));

        // and the body
        List<Statement> body = this.parseBody();
        this.functions.put(name, new Function(args, type, body, this.fileName, line, false));

    }

    public void parse() throws CompileException {
        advance();
        // the only things we allow in the outer scope are
        // #include, struct foo, #define, #extern, ; or int add()
        while(!matchType(TokenType.TOKEN_EOF)){
            if(matchType(TokenType.TOKEN_INCLUDE)){
                this.parseInclude();
            }else if(matchType(TokenType.TOKEN_STRUCT)){
                this.parseStructDeclaration();
            }else if(matchType(TokenType.TOKEN_DEFINE)){
                this.parseDefine();
            }else if(matchType(TokenType.TOKEN_EXTERN)){
                this.parseExtern();
                // note the '!' here, to avoid trying to parse empty ';'
            } else if(!matchType(TokenType.TOKEN_SEMICOLON)){
                this.parseFunction();
            }
        }

        logger.info(String.format("Parsed %s, got %s tokens",fileName, tokenCount));
    }
    private Expression parseGroupedExpression(Expression expression, boolean canAssign, int line) throws CompileException{
        if(isVariableType(this.current.type())){
            DataType type = parseType();
            consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after cast expression");
            return new CastExpression(type, this.parseEmptyExpression(), line, fileName);
        }

        Expression groupedExpression = this.parseEmptyExpression();
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after grouped expression");
        return new GroupedExpression(groupedExpression, line, this.fileName);
    }
    private Expression parseDotExpression(Expression expression, boolean canAssign, int line) throws CompileException{
        consume(TokenType.TOKEN_IDENTIFIER, "Expected identifier after dot");
        return new DotExpression(expression, this.previous.literal(), line, this.fileName);
    }

    private Expression parseRightSideOfBinary(Token op) throws CompileException{
        ParseFunction rule = this.parseFunctions.get(op.type());
        return parseExpr(null, Precedence.values()[rule.precedence.ordinal() + 1]);
    }
    private Expression parseComparison(Expression left, boolean canAssign, int line) throws CompileException{
        Token op = this.previous;
        return new ComparisonExpression(left, parseRightSideOfBinary(this.previous), op, line, this.fileName);
    }

    private Expression parseBinary(Expression expression, boolean canAssign, int line) throws CompileException{
        return new BinaryExpression(expression, this.previous, parseRightSideOfBinary(this.previous), line, this.fileName);
    }
    private Expression parseDereference(Expression expression, boolean canAssign, int line) throws CompileException{
        Token op = this.previous;
        if(matchType(TokenType.TOKEN_STAR)){
            return new UnaryExpression(parseDereference(null, canAssign, line), op, line, this.fileName);
        }
        Precedence precedence = canAssign ? Precedence.ASSIGNMENT : Precedence.OR;
        return new UnaryExpression(parseExpr(null, precedence), op, line, this.fileName);
    }

    private Expression parseUnary(Expression expression, boolean canAssign, int line) throws CompileException{
        Token op = this.previous;
        Expression newExpression = parseExpr(null, Precedence.UNARY);
        return new UnaryExpression(newExpression, op, line, this.fileName);
    }
    private List<Expression> parseList(TokenType endToken, String errorMsg) throws CompileException {
        List<Expression> args = new ArrayList<>();
        if(this.current.type() != endToken){
            do{
                args.add(this.parseEmptyExpression());
            }while(matchType(TokenType.TOKEN_COMMA));
        }
        consume(endToken, errorMsg);
        return args;
    }
    private Expression parseCall(String variable) throws CompileException {
        int line = this.scanner.getLine();
        List<Expression> arguments = this.parseList(TokenType.TOKEN_RIGHT_PAREN, "Expect right paren after args");
        return new CallExpression(variable, arguments, line, this.fileName);
    }

    private Expression parseVariable(Expression expression, boolean canAssign, int line) throws CompileException{
        String variable = this.previous.literal();
        if(matchType(TokenType.TOKEN_LEFT_PAREN)){
            return this.parseCall(variable);
        }
        return new VariableExpression(variable, line, this.fileName);
    }
    private Expression parseLogical(Expression expression, boolean canAssign, int line) throws CompileException{
        Token op = this.previous;
        Expression newExpression = parseExpr(null, Precedence.AND);
        return new LogicalExpression(expression, newExpression, op, line, this.fileName);
    }

    private List<Expression> parseArrayItems(int size) throws CompileException{
        int line = this.scanner.getLine();
        List<Expression> items = this.parseList(TokenType.TOKEN_RIGHT_BRACKET, String.format("Unexpected token %s, expected ']' or ','", this.current));
        if(size < items.size()){
            panic(String.format("Array declaration has excess element! Expected %d got %d", size, items.size()), line);
        }
       return items;
    }

    private Expression parseIndex(Expression expression, boolean canAssign, int line) throws CompileException{
        Expression out = new IndexExpression(expression, this.parseEmptyExpression(), line, this.fileName);
        consume(TokenType.TOKEN_RIGHT_BRACKET, "Expected ']' after index");
        return out;
    }
    public Parser(Scanner scanner, List<String> included, String fileName, FileHandler fileHandler){
        this(scanner, included, new HashMap<>(), fileName, fileHandler);
    }
    public Parser(Scanner scanner, List<String> included, Map<String, Structure> structures, String fileName, FileHandler fileHandler){
        this.included       = included;
        this.definedQueue   = new ArrayDeque<>();
        this.defined        = new HashMap<>();
        this.functions      = new HashMap<>();
        this.structures     = structures;
        this.scanner        = scanner;
        this.fileName       = fileName;
        this.current        = null;
        this.previous       = null;
        this.tokenCount     = 0;
        this.logger         = Logger.getLogger("parser");
        this.fileHandler    = fileHandler;
        this.logger.addHandler(this.fileHandler);
    }
}
