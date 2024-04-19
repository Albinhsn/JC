package se.liu.albhe576.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Parser {
    @FunctionalInterface
    private interface InfixRule{
        Expr infix(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException;
    }
    @FunctionalInterface
    private interface PrefixRule{
        Expr prefix(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException;
    }
    private static class ParseFunction{
        PrefixRule prefixRule;
        InfixRule infixRule;
        Precedence precedence;
        public ParseFunction(PrefixRule prefixRule, InfixRule infixRule, Precedence precedence){
            this.prefixRule = prefixRule;
            this.infixRule = infixRule;
            this.precedence = precedence;

        }
    }

    private final EnumMap<TokenType, ParseFunction> parseFunctions;
    private final Map<String, Token> defined;
    private final List<String> included;
    private final List<Function> extern;
    private final Map<String, Struct> structs;
    private final Scanner scanner;
    private Token current;
    private Token previous;
    private List<Stmt> stmts;
    public List<Function> getExtern(){
        return this.extern;
    }
    public Map<String, Struct> getStructs(){
        return this.structs;
    }

    private void updateCurrent(){
        if(this.current.type == TokenType.TOKEN_IDENTIFIER && this.defined.containsKey(this.current.literal)){
            Token value = this.defined.get(this.current.literal);
            this.current.type = value.type;
            this.current.literal = value.literal;
        }
    }

    private void advance() throws IllegalCharacterException, UnterminatedStringException {
        previous = current;
        current = scanner.parseToken();
        this.updateCurrent();
    }



    private boolean matchType(TokenType type) throws IllegalCharacterException, UnterminatedStringException {
        if(this.current.type != type){
           return false;
        }
        advance();
        return true;
    }

    private void error(String s){
        System.out.printf("ERROR: %s\n", s);
        System.exit(1);
    }

    private boolean isVariableType() {
        switch(this.current.type){
            case TOKEN_INT:{}
            case TOKEN_FLOAT:{}
            case TOKEN_VOID:{
                return true;
            }
        }
        return false;
    }

    private DataType parsePointerType(DataType type) throws CompileException, IllegalCharacterException, UnterminatedStringException {
        if(matchType(TokenType.TOKEN_STAR)){
            type = DataType.getPointerFromType(type);
            while(matchType(TokenType.TOKEN_STAR)){
                type = DataType.getPointerFromType(type);
            }
        }
        return type;
    }

    private DataType parseType() throws CompileException, IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        Token type = this.current;
        DataType dataType = DataType.getDataTypeFromToken(type);
        advance();
        return this.parsePointerType(dataType);
    }

    private StructField parseStructField() throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException, CompileException {

        Token type = this.current;
        DataType dataType = this.parseType();
        if(!matchType(TokenType.TOKEN_IDENTIFIER)){
            throw new UnexpectedTokenException(String.format("Expected identifier as argument but got %s", this.current.type));
        }
        String name = this.previous.literal;
        return new StructField(name, dataType, type.literal);
    }

    private void structDeclaration() throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException, CompileException {
        if(!matchType(TokenType.TOKEN_IDENTIFIER)){
            error(String.format("Expected identifier but got %s", this.current.type));
        }
        Token name = this.previous;
        if(!matchType(TokenType.TOKEN_LEFT_BRACE)){
            error(String.format("Expected left brace but got %s", this.current.type));
        }

        List<StructField> fields = new ArrayList<>();
        while(!matchType(TokenType.TOKEN_RIGHT_BRACE)){
            fields.add(this.parseStructField());
            if(!matchType(TokenType.TOKEN_SEMICOLON)){
                throw new UnexpectedTokenException(String.format("Expected semicolon after struct field but got %s", this.current.type));
            }
        }
        this.structs.put(name.literal, new Struct(name.literal, fields));
    }

    private void consume(TokenType type, String msg) throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        if(!matchType(type)){
            throw new UnexpectedTokenException(msg);
        }
    }
    private Expr literal(Expr expr, boolean canAssign) {
        return new LiteralExpr(this.previous, this.previous.line);
    }


    private Expr parseExpr(Expr expr, Precedence precedence) throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {

        advance();
        ParseFunction prefix = this.parseFunctions.getOrDefault(this.previous.type, null);
        if(prefix == null || prefix.prefixRule == null){
            error(String.format("Expected expression but got %s %s", this.previous.literal, precedence));
        }
        assert prefix != null;

        final int precedenceOrdinal = precedence.ordinal();
        final boolean canAssign =  precedenceOrdinal <= Precedence.ASSIGNMENT.ordinal();
        expr = prefix.prefixRule.prefix(expr, canAssign);

        ParseFunction currentRule = this.parseFunctions.get(this.current.type);
        while(precedenceOrdinal <= currentRule.precedence.ordinal()){
            advance();
            expr = currentRule.infixRule.infix(expr, canAssign);
            currentRule = this.parseFunctions.get(this.current.type);
        }

        if(canAssign && matchType(TokenType.TOKEN_EQUAL)){
            error("Invalid assignment target");
        }
        return expr;
    }

    private boolean matchAugmented() throws IllegalCharacterException, UnterminatedStringException {
        return (
                matchType(TokenType.TOKEN_AUGMENTED_MINUS) ||
                matchType(TokenType.TOKEN_AUGMENTED_STAR)  ||
                matchType(TokenType.TOKEN_AUGMENTED_SLASH) ||
                matchType(TokenType.TOKEN_AUGMENTED_PLUS)
        );
    }

    private Stmt expressionStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException, CompileException {
        if(this.structs.containsKey(this.current.literal)){
            DataType type = this.parseType();
            consume(TokenType.TOKEN_IDENTIFIER, String.format("Expected identifier for type at line %d", this.current.line));
            String name = this.previous.literal;
            if(this.current.type == TokenType.TOKEN_SEMICOLON){
                return new VariableStmt(type, name, null, this.current.line);
            }
            consume(TokenType.TOKEN_EQUAL, String.format("Expected '=' or ';' after struct variable on line %d", this.current.line));
            if(matchType(TokenType.TOKEN_LEFT_BRACKET)){
                return array(type, name);
            }
            return new VariableStmt(type, name, parseExpr(new EmptyExpr(this.current.line), Precedence.ASSIGNMENT), this.current.line);

        }

        int line = this.current.line;
        Expr expr = parseExpr(new EmptyExpr(line), Precedence.OR);
        if(matchType(TokenType.TOKEN_EQUAL)) {
            return new ExprStmt(new AssignExpr(expr, parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT), line), line);
        }
        else if(matchAugmented()){
            return new ExprStmt(new AugmentedExpr(this.previous, expr, parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT), line), line);
        }
        return new ExprStmt(expr, line);
    }

    private Stmt returnStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        this.advance();
        if(matchType(TokenType.TOKEN_SEMICOLON)){
            return new ReturnStmt(new EmptyExpr(this.previous.line), this.previous.line);
        }
        Stmt out = new ReturnStmt(parseExpr(new EmptyExpr(this.current.line), Precedence.ASSIGNMENT), this.previous.line);
        consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after return stmt %s", out));
        return out;
    }
    private Stmt forStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException, CompileException {
        this.advance();
        int line = this.previous.line;
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected '(' after for");
        Stmt init = parseStmt();
        Stmt condition = expressionStatement();
        consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after condition stmt %s", condition));
        Stmt update = expressionStatement();
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after loop control variables");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' in for loop");

        List<Stmt> body = this.parseBody();

        return new ForStmt(init, condition, update, body, line);

    }

    private Stmt whileStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException, CompileException {
        this.advance();
        int line = this.previous.line;
        consume(TokenType.TOKEN_LEFT_PAREN, "expected '(' after while");
        Expr condition = parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT);
        consume(TokenType.TOKEN_RIGHT_PAREN, "expected ')' after while");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after while condition");

        List<Stmt> body = parseBody();
        return new WhileStmt(condition, body, line);
    }


    private List<Stmt> parseBody() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException, CompileException {
        List<Stmt> body = new ArrayList<>();
        while(!matchType(TokenType.TOKEN_RIGHT_BRACE)) {
            body.add(parseStmt());
        }
        return body;
    }
    private List<StructField> parseArguments() throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException, CompileException {
        List<StructField> args = new ArrayList<>();
        if(this.current.type != TokenType.TOKEN_RIGHT_PAREN){
            do{
                args.add(this.parseStructField());
            }while(matchType(TokenType.TOKEN_COMMA));
        }
        return args;
    }

    private Stmt function(DataType type, String name, int line) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException, CompileException {
        List<StructField> args = this.parseArguments();

        consume(TokenType.TOKEN_RIGHT_PAREN, String.format("Expected right paren after arguments but got %s", this.current.type));
        consume(TokenType.TOKEN_LEFT_BRACE, String.format("Expected { to start function body but got %s", this.current.type));

        List<Stmt> body = this.parseBody();
        return new FunctionStmt(type, name,args, body, line);
    }

    private Stmt variableDeclaration() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException, CompileException {
        int line = this.current.line;
        DataType type = this.parseType();

        consume(TokenType.TOKEN_IDENTIFIER, String.format("Expected identifier after variable type but got %s", this.current.type));
        String name = this.previous.literal;

        if(matchType(TokenType.TOKEN_EQUAL)){
            if(matchType(TokenType.TOKEN_LEFT_BRACKET)){
                Stmt out = array(type, name);
                consume(TokenType.TOKEN_SEMICOLON, String.format("Expected semicolon after declaring an array on line %d", line));
                return out;
            }

            Expr value = parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT);
            consume(TokenType.TOKEN_SEMICOLON, String.format("expected semicolon after assign expr but got %s", this.current.type));
            return new VariableStmt(type, name,value, line);

        }else if(matchType(TokenType.TOKEN_LEFT_PAREN)){
            return this.function(type, name, line);
        }else if(matchType(TokenType.TOKEN_SEMICOLON)){
            return new VariableStmt(type, name, null, line);
        }

        throw new UnexpectedTokenException(String.format("Expected '=' or '(' after variable but got %s", this.current.type));

    }
    private Stmt ifStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException, CompileException {
        this.advance();
        int line = this.previous.line;
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected '(' after if");
        Expr condition = parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT);

        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after if condition");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after if condition");

        List<Stmt> ifBody = parseBody();
        List<Stmt> elseBody;

        if(matchType(TokenType.TOKEN_ELSE)){
            consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after if condition");
            elseBody = parseBody();
        }else{
            elseBody = new ArrayList<>();
        }

        return new IfStmt(condition, ifBody, elseBody, line);
    }
    private Stmt parseStmt() throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException, CompileException {
        switch(this.current.type){
            case TOKEN_FOR -> {
                return forStatement();
            }
            case TOKEN_WHILE -> {
                return whileStatement();
            }
            case TOKEN_IF-> {
                return ifStatement();
            }
            case TOKEN_RETURN-> {
                return returnStatement();
            }
            default -> {
                if(isVariableType()){
                    return variableDeclaration();
                }
                Stmt out = expressionStatement();
                consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after expression Stmt %s", out));
                return out;
            }

        }
    }


    private void parseInclude() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException, IOException, CompileException {
        consume(TokenType.TOKEN_STRING, "Expected string after include?");
        String fileName = this.previous.literal;

        if(this.included.stream().anyMatch(i -> i.equals(fileName))){
            System.out.printf("already included %s\n%n", fileName);
            return;
        }

        this.included.add(fileName);
        String s = Files.readString(Path.of(fileName));

        Parser includeParser = new Parser(new Scanner(s), this.included);
        List<Stmt> included = includeParser.parse();
        for(Map.Entry<String, Struct> entry : includeParser.structs.entrySet()){
            String key = entry.getKey();
            Struct value = entry.getValue();

            if(this.structs.containsKey(key)){
                throw new CompileException(String.format("Imported struct '%s' which is already declared from file %s", key, fileName));
            }
            this.structs.put(key, value);
        }
        included.addAll(this.stmts);
        this.stmts = included;
    }

    private void parseDefine() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        consume(TokenType.TOKEN_IDENTIFIER, "expected identifier after define");
        this.defined.put(this.previous.literal, this.current);
        advance();
    }
    private void parseExtern() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException, CompileException {
        DataType type;
        if(isVariableType()){
            type = DataType.getDataTypeFromToken(this.current);
            advance();
        }else{
            consume(TokenType.TOKEN_IDENTIFIER, "Expected identifier for function name after #extern");
            type = DataType.getDataTypeFromToken(this.previous);
        }
        type = this.parsePointerType(type);
        if(matchType(TokenType.TOKEN_STAR)){
            type = DataType.getPointerFromType(type);
            while(matchType(TokenType.TOKEN_STAR)){
                type = DataType.getPointerFromType(type);
            }
        }

        consume(TokenType.TOKEN_IDENTIFIER, "Expected identifier for function name after #extern");
        String funcName = this.previous.literal;
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected ( for function args after function name in extern");
        if(matchType(TokenType.TOKEN_ELLIPSIS)){
            this.extern.add(new Function(funcName, type, null));
        }else{
            List<StructField> args = this.parseArguments();
            this.extern.add(new Function(funcName, args, type, null));
        }
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ) after function args in extern");

    }

    public List<Stmt> parse(){
        try{
            advance();
            while(!matchType(TokenType.TOKEN_EOF)){
                if(matchType(TokenType.TOKEN_INCLUDE)){
                    this.parseInclude();
                }else if(matchType(TokenType.TOKEN_STRUCT)){
                    this.structDeclaration();
                }else if(matchType(TokenType.TOKEN_DEFINE)){
                    this.parseDefine();
                }else if(matchType(TokenType.TOKEN_EXTERN)){
                    this.parseExtern();
                }
                else{
                    this.stmts.add(parseStmt());

                }
            }
        }catch(IllegalCharacterException | UnterminatedStringException | UnexpectedTokenException | CompileException |
               IOException e){
            System.out.println(e.getMessage());
        }
        return this.stmts;
    }
    private Expr grouping(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        int line = this.previous.line;
        Expr groupedExpr = parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT);
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after grouped expression");
        return new GroupedExpr(groupedExpr, line);
    }
    private Expr dot(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        int line = this.previous.line;
        consume(TokenType.TOKEN_IDENTIFIER, "Expected identifier after dot");
        Token var = this.previous;

        return new DotExpr(expr, var, line);
    }
    private Expr comparison(Expr left, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        int line = this.previous.line;
        Token op = this.previous;
        ParseFunction rule = this.parseFunctions.get(op.type);
        Expr right = parseExpr(new EmptyExpr(line), Precedence.values()[rule.precedence.ordinal() + 1]);
        return new ComparisonExpr(left, right, op, line);
    }

    private Expr binary(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        Token op = this.previous;
        int line = this.previous.line;
        ParseFunction rule = this.parseFunctions.get(op.type);
        Expr newExpr = parseExpr(new EmptyExpr(line), Precedence.values()[rule.precedence.ordinal() + 1]);
        return new BinaryExpr(expr, op, newExpr, line);
    }
    private Expr dereference(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        Token op = this.previous;
        int line = this.previous.line;
        UnaryExpr headExpr = new UnaryExpr(new EmptyExpr(line), op, line);
        UnaryExpr unaryExpr = headExpr;
        while(matchType(TokenType.TOKEN_STAR)){
            unaryExpr.expr = new UnaryExpr(new EmptyExpr(line), op, line);
            unaryExpr = (UnaryExpr) unaryExpr.expr;
        }
        Precedence precedence = canAssign ? Precedence.ASSIGNMENT : Precedence.OR;
        unaryExpr.expr = parseExpr(new EmptyExpr(line), precedence);
        return headExpr;
    }

    private Expr unary(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        Token op = this.previous;
        int line = this.previous.line;
        Expr newExpr = parseExpr(new EmptyExpr(line), Precedence.UNARY);
        return new UnaryExpr(newExpr, op, line);
    }
    private Expr parseCall(Token var) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        int line = this.previous.line;
        List<Expr> args = new ArrayList<>();
        if(this.current.type != TokenType.TOKEN_RIGHT_PAREN){
            do{
                args.add(parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT));
            }while(matchType(TokenType.TOKEN_COMMA));
        }
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expect right paren after args");
        return new CallExpr(var, args, line);
    }
    private Expr variable(Expr expr, boolean canAssign) throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        Token var = this.previous;
        int line = this.previous.line;
        if(matchType(TokenType.TOKEN_DECREMENT) || matchType(TokenType.TOKEN_INCREMENT)){
            return new PostfixExpr(var, this.previous, line);
        }
        else if(matchType(TokenType.TOKEN_LEFT_PAREN)){
            return this.parseCall(var);
        }else if(canAssign && matchType(TokenType.TOKEN_EQUAL)){
            return new AssignExpr(new VarExpr(var, line), parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT), line);
        }else if (canAssign && matchAugmented()){
            return new AugmentedExpr(this.previous, new VarExpr(var, line), parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT), line);
        }
        return new VarExpr(var, line);
    }
    private Expr logical(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        Token op = this.previous;
        int line = this.previous.line;
        Expr newExpr = parseExpr(new EmptyExpr(line), Precedence.AND);
        return new LogicalExpr(expr, newExpr, op, line);
    }

    private Stmt array(DataType type, String name) throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        int line = this.previous.line;
        List<Expr> items = new ArrayList<>();
        if(!matchType(TokenType.TOKEN_RIGHT_BRACKET)){
            while(true){
                items.add(parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT));
                if(!matchType(TokenType.TOKEN_COMMA)){
                    consume(TokenType.TOKEN_RIGHT_BRACKET, String.format("Unexpected token %s, expected ']' or ','", this.current));
                    break;
                }

            }
        }
       return new ArrayStmt(type, name, items, line);
    }

    private Expr index(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        int line = this.previous.line;
        Expr out = new IndexExpr(expr, parseExpr(new EmptyExpr(line), Precedence.ASSIGNMENT), line);
        consume(TokenType.TOKEN_RIGHT_BRACKET, "Expected ']' after index");
        return out;
    }


    private void buildParseFunctionMap(){
        this.parseFunctions.put(TokenType.TOKEN_LEFT_BRACKET, new ParseFunction(null, this::index, Precedence.CALL));
        this.parseFunctions.put(TokenType.TOKEN_RIGHT_BRACKET, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_LEFT_PAREN, new ParseFunction(this::grouping, null, Precedence.CALL));
        this.parseFunctions.put(TokenType.TOKEN_RIGHT_PAREN, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_LEFT_BRACE, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_RIGHT_BRACE, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_COMMA, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_DOT, new ParseFunction(null, this::dot, Precedence.CALL));
        this.parseFunctions.put(TokenType.TOKEN_AUGMENTED_SLASH, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_AUGMENTED_STAR, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_AUGMENTED_MINUS, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_AUGMENTED_PLUS, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_MINUS, new ParseFunction(this::unary, this::binary, Precedence.TERM));
        this.parseFunctions.put(TokenType.TOKEN_PLUS, new ParseFunction(null, this::binary, Precedence.TERM));
        this.parseFunctions.put(TokenType.TOKEN_SEMICOLON, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_SLASH, new ParseFunction(null, this::binary, Precedence.FACTOR));
        this.parseFunctions.put(TokenType.TOKEN_STAR, new ParseFunction(this::dereference, this::binary, Precedence.FACTOR));
        this.parseFunctions.put(TokenType.TOKEN_BANG, new ParseFunction(this::unary, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_BANG_EQUAL, new ParseFunction(null, this::comparison, Precedence.EQUALITY));
        this.parseFunctions.put(TokenType.TOKEN_EQUAL, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_EQUAL_EQUAL, new ParseFunction(null, this::comparison, Precedence.EQUALITY));
        this.parseFunctions.put(TokenType.TOKEN_GREATER, new ParseFunction(null, this::comparison, Precedence.COMPARISON));
        this.parseFunctions.put(TokenType.TOKEN_GREATER_EQUAL, new ParseFunction(null, this::comparison, Precedence.COMPARISON));
        this.parseFunctions.put(TokenType.TOKEN_LESS, new ParseFunction(null, this::comparison, Precedence.COMPARISON));
        this.parseFunctions.put(TokenType.TOKEN_LESS_EQUAL, new ParseFunction(null, this::comparison, Precedence.COMPARISON));
        this.parseFunctions.put(TokenType.TOKEN_IDENTIFIER, new ParseFunction(this::variable, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_STRING, new ParseFunction(this::literal, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_INT_LITERAL, new ParseFunction(this::literal, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_FLOAT_LITERAL, new ParseFunction(this::literal, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_AND_LOGICAL, new ParseFunction(null, this::logical, Precedence.AND));
        this.parseFunctions.put(TokenType.TOKEN_AND_BIT, new ParseFunction(this::unary, this::binary, Precedence.BITWISE));
        this.parseFunctions.put(TokenType.TOKEN_OR_LOGICAL, new ParseFunction(null, this::logical, Precedence.OR));
        this.parseFunctions.put(TokenType.TOKEN_OR_BIT, new ParseFunction(null, this::binary, Precedence.BITWISE));
        this.parseFunctions.put(TokenType.TOKEN_XOR, new ParseFunction(null, this::binary, Precedence.BITWISE));
        this.parseFunctions.put(TokenType.TOKEN_SHIFT_LEFT, new ParseFunction(null, this::binary, Precedence.BITWISE));
        this.parseFunctions.put(TokenType.TOKEN_SHIFT_RIGHT, new ParseFunction(null, this::binary, Precedence.BITWISE));
        this.parseFunctions.put(TokenType.TOKEN_TRUE, new ParseFunction(this::literal, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_FALSE, new ParseFunction(this::literal, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_INCREMENT, new ParseFunction(this::unary, null, Precedence.TERM));
        this.parseFunctions.put(TokenType.TOKEN_DECREMENT, new ParseFunction(this::unary, null, Precedence.TERM));
        this.parseFunctions.put(TokenType.TOKEN_MOD, new ParseFunction(null, this::binary, Precedence.TERM));
        this.parseFunctions.put(TokenType.TOKEN_ELLIPSIS, new ParseFunction(null, null, Precedence.NONE));
    }

    public Parser(Scanner scanner, List<String> included){
        this.included = included;
        this.stmts = new ArrayList<>();
        this.defined = new HashMap<>();
        this.extern = new ArrayList<>();
        this.structs = new HashMap<>();
        this.scanner = scanner;
        this.current = null;
        this.previous = null;


        this.parseFunctions = new EnumMap<>(TokenType.class);
        this.buildParseFunctionMap();
    }
}
