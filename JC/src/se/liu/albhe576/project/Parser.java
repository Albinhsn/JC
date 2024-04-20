package se.liu.albhe576.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Parser {
    @FunctionalInterface
    private interface InfixRule{
        Expr infix(Expr expr, boolean canAssign) throws CompileException;
    }
    @FunctionalInterface
    private interface PrefixRule{
        Expr prefix(Expr expr, boolean canAssign) throws CompileException;
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

    private Expr getEmptyExpr(int line){
        return new Expr(line, this.getCurrentFile());
    }

    private final EnumMap<TokenType, ParseFunction> parseFunctions;
    private final Map<String, Token> defined;
    private final List<String> included;
    private final String fileName;
    private final List<Function> extern;
    private final Map<String, Struct> structs;
    private final Scanner scanner;
    private Token current;
    private Token previous;
    private final List<Stmt> stmts;

    public String getCurrentFile(){
        return this.fileName;
    }
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

    private void advance() throws CompileException{
        previous = current;
        current = scanner.parseToken();
        this.updateCurrent();
    }

    private boolean matchType(TokenType type) throws CompileException{
        if(this.current.type != type){
           return false;
        }
        advance();
        return true;
    }

    private void error(String s){
        System.out.printf(String.format("%s:%d[%s]", this.getCurrentFile(),this.scanner.getLine(),s));
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

    private DataType parsePointerType(DataType type) throws CompileException{
        if(matchType(TokenType.TOKEN_STAR)){
            type = DataType.getPointerFromType(type);
            while(matchType(TokenType.TOKEN_STAR)){
                type = DataType.getPointerFromType(type);
            }
        }
        return type;
    }

    private DataType parseType() throws CompileException{
        Token type = this.current;
        DataType dataType = DataType.getDataTypeFromToken(type);
        advance();
        return this.parsePointerType(dataType);
    }

    private StructField parseStructField() throws CompileException{

        Token type = this.current;
        DataType dataType = this.parseType();
        if(!matchType(TokenType.TOKEN_IDENTIFIER)){
            this.error(String.format("Expected identifier as argument but got %s", this.current.type));
        }
        String name = this.previous.literal;
        return new StructField(name, dataType, type.literal);
    }

    private void structDeclaration() throws CompileException {
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
                this.error(String.format("Expected semicolon after struct field but got %s", this.current.type));
            }
        }
        if(this.structs.containsKey(name.literal)){
            Struct struct = this.structs.get(name.literal);
            this.error(String.format("Can't declare struct '%s' which is already declared from file %s", name.literal, struct.fileName));
        }
        this.structs.put(name.literal, new Struct(name.literal, fields, this.getCurrentFile()));
    }

    private void consume(TokenType type, String msg) throws CompileException{
        if(!matchType(type)){
            this.error(msg);
        }
    }
    private Expr literal(Expr expr, boolean canAssign) {
        return new LiteralExpr(this.previous, this.previous.line, this.getCurrentFile());
    }

    private Expr parseExpr(Expr expr, Precedence precedence) throws CompileException{

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

    private boolean matchAugmented() throws CompileException{
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

    private Stmt expressionStatement() throws CompileException {

        int line = this.current.line;
        Expr expr = parseExpr(this.getEmptyExpr(line), Precedence.OR);
        String file = this.getCurrentFile();
        if(matchType(TokenType.TOKEN_EQUAL)) {
            return new ExprStmt(new AssignExpr(expr, parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT), line, file), line, file);
        }
        else if(matchAugmented()){
            return new ExprStmt(new AugmentedExpr(this.previous, expr, parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT), line, file), line, file);
        }
        return new ExprStmt(expr, line, file);
    }

    private Stmt returnStatement() throws CompileException{
        this.advance();
        if(matchType(TokenType.TOKEN_SEMICOLON)){
            return new ReturnStmt(null, this.previous.line, this.getCurrentFile());
        }
        Stmt out = new ReturnStmt(parseExpr(this.getEmptyExpr(this.current.line), Precedence.ASSIGNMENT), this.previous.line, this.getCurrentFile());
        consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after return stmt %s", out));
        return out;
    }
    private Stmt forStatement() throws CompileException {
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

        return new ForStmt(init, condition, update, body, line, this.getCurrentFile());

    }

    private Stmt whileStatement() throws CompileException {
        this.advance();
        int line = this.previous.line;
        consume(TokenType.TOKEN_LEFT_PAREN, "expected '(' after while");
        Expr condition = parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT);
        consume(TokenType.TOKEN_RIGHT_PAREN, "expected ')' after while");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after while condition");

        List<Stmt> body = parseBody();
        return new WhileStmt(condition, body, line, this.getCurrentFile());
    }


    private List<Stmt> parseBody() throws CompileException {
        List<Stmt> body = new ArrayList<>();
        while(!matchType(TokenType.TOKEN_RIGHT_BRACE)) {
            body.add(parseStmt());
        }
        return body;
    }
    private List<StructField> parseArguments() throws CompileException {
        List<StructField> args = new ArrayList<>();
        if(this.current.type != TokenType.TOKEN_RIGHT_PAREN){
            do{
                args.add(this.parseStructField());
            }while(matchType(TokenType.TOKEN_COMMA));
        }
        return args;
    }

    private Stmt function(DataType type, String name, int line) throws CompileException {
        List<StructField> args = this.parseArguments();

        consume(TokenType.TOKEN_RIGHT_PAREN, String.format("Expected right paren after arguments but got %s", this.current.type));
        consume(TokenType.TOKEN_LEFT_BRACE, String.format("Expected { to start function body but got %s", this.current.type));

        List<Stmt> body = this.parseBody();
        return new FunctionStmt(type, name,args, body, line, this.getCurrentFile());
    }

    private Stmt variableDeclaration() throws CompileException {
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

            Expr value = parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT);
            consume(TokenType.TOKEN_SEMICOLON, String.format("expected semicolon after assign expr but got %s", this.current.type));
            return new VariableStmt(type, name,value, line, this.getCurrentFile());

        }else if(matchType(TokenType.TOKEN_LEFT_PAREN)){
            return this.function(type, name, line);
        }else if(matchType(TokenType.TOKEN_SEMICOLON)){
            return new VariableStmt(type, name, null, line, this.getCurrentFile());
        }

        this.error(String.format("Expected '=' or '(' after variable but got %s", this.current.type));
        // unreachable
        return null;
    }
    private Stmt ifStatement() throws CompileException {
        this.advance();
        int line = this.previous.line;
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected '(' after if");
        Expr condition = parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT);

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

        return new IfStmt(condition, ifBody, elseBody, line, this.getCurrentFile());
    }
    private Stmt parseStmt() throws CompileException {
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
                }else if(this.structs.containsKey(this.current.literal)){
                        DataType type = this.parseType();
                        consume(TokenType.TOKEN_IDENTIFIER, String.format("Expected identifier for type at line %d", this.current.line));
                        String name = this.previous.literal;
                        if(this.current.type == TokenType.TOKEN_SEMICOLON){
                            Stmt out = new VariableStmt(type, name, null, this.current.line, this.getCurrentFile());
                            consume(TokenType.TOKEN_SEMICOLON, "Expected ';' after variable");
                            return out;
                        }
                        if(matchType(TokenType.TOKEN_LEFT_PAREN)){
                            return function(type, name, this.previous.line);
                        }
                        consume(TokenType.TOKEN_EQUAL, String.format("Expected '=' or ';' after struct variable on line %d", this.current.line));
                        if(matchType(TokenType.TOKEN_LEFT_BRACKET)){
                            Stmt out = array(type, name);
                            consume(TokenType.TOKEN_SEMICOLON, "Expected ';' after array");
                            return out;
                        }
                        Stmt out = new VariableStmt(type, name, parseExpr(this.getEmptyExpr(this.current.line), Precedence.ASSIGNMENT), this.current.line, this.getCurrentFile());
                        consume(TokenType.TOKEN_SEMICOLON, "Expected ';' after variable");
                        return out;
                }

                Stmt out = expressionStatement();
                consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after expression Stmt %s", out));
                return out;
            }

        }
    }


    private void parseInclude() throws IOException, CompileException {
        consume(TokenType.TOKEN_STRING, "Expected string after include?");
        String fileName = this.previous.literal;

        if(this.included.stream().anyMatch(i -> i.equals(fileName))){
            System.out.printf("already included %s\n%n", fileName);
            return;
        }

        this.included.add(fileName);
        String s = Files.readString(Path.of(fileName));

        Parser includeParser = new Parser(new Scanner(s), this.included, this.structs, fileName);
        List<Stmt> included = includeParser.parse();
        for(Map.Entry<String, Struct> entry : includeParser.structs.entrySet()){
            String key = entry.getKey();
            Struct value = entry.getValue();

            this.structs.put(key, value);
        }
        this.stmts.addAll(included);

    }

    private void parseDefine() throws CompileException {
        consume(TokenType.TOKEN_IDENTIFIER, "expected identifier after define");
        this.defined.put(this.previous.literal, this.current);
        advance();
    }
    private void parseExtern() throws CompileException {
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
        }catch(CompileException |
               IOException e){
            System.out.println(e.getMessage());
        }
        return this.stmts;
    }
    private Expr grouping(Expr expr, boolean canAssign) throws CompileException{
        int line = this.previous.line;
        Expr groupedExpr = parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT);
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after grouped expression");
        return new GroupedExpr(groupedExpr, line, this.getCurrentFile());
    }
    private Expr dot(Expr expr, boolean canAssign) throws CompileException{
        int line = this.previous.line;
        consume(TokenType.TOKEN_IDENTIFIER, "Expected identifier after dot");
        Token var = this.previous;

        return new DotExpr(expr, var, line, this.getCurrentFile());
    }
    private Expr comparison(Expr left, boolean canAssign) throws CompileException{
        int line = this.previous.line;
        Token op = this.previous;
        ParseFunction rule = this.parseFunctions.get(op.type);
        Expr right = parseExpr(this.getEmptyExpr(line), Precedence.values()[rule.precedence.ordinal() + 1]);
        return new ComparisonExpr(left, right, op, line, this.getCurrentFile());
    }

    private Expr binary(Expr expr, boolean canAssign) throws CompileException{
        Token op = this.previous;
        int line = this.previous.line;
        ParseFunction rule = this.parseFunctions.get(op.type);
        Expr newExpr = parseExpr(this.getEmptyExpr(line), Precedence.values()[rule.precedence.ordinal() + 1]);
        return new BinaryExpr(expr, op, newExpr, line, this.getCurrentFile());
    }
    private Expr dereference(Expr expr, boolean canAssign) throws CompileException{
        Token op = this.previous;
        int line = this.previous.line;
        UnaryExpr headExpr = new UnaryExpr(this.getEmptyExpr(line), op, line, this.getCurrentFile());
        UnaryExpr unaryExpr = headExpr;
        while(matchType(TokenType.TOKEN_STAR)){
            unaryExpr.expr = new UnaryExpr(this.getEmptyExpr(line), op, line, this.getCurrentFile());
            unaryExpr = (UnaryExpr) unaryExpr.expr;
        }
        Precedence precedence = canAssign ? Precedence.ASSIGNMENT : Precedence.OR;
        unaryExpr.expr = parseExpr(this.getEmptyExpr(line), precedence);
        return headExpr;
    }

    private Expr unary(Expr expr, boolean canAssign) throws CompileException{
        Token op = this.previous;
        int line = this.previous.line;
        Expr newExpr = parseExpr(this.getEmptyExpr(line), Precedence.UNARY);
        return new UnaryExpr(newExpr, op, line, this.getCurrentFile());
    }
    private Expr parseCall(Token var) throws CompileException {
        int line = this.previous.line;
        List<Expr> args = new ArrayList<>();
        if(this.current.type != TokenType.TOKEN_RIGHT_PAREN){
            do{
                args.add(parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT));
            }while(matchType(TokenType.TOKEN_COMMA));
        }
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expect right paren after args");
        return new CallExpr(var, args, line, this.getCurrentFile());
    }
    private Expr variable(Expr expr, boolean canAssign) throws CompileException{
        Token var = this.previous;
        int line = this.previous.line;
        if(matchType(TokenType.TOKEN_DECREMENT) || matchType(TokenType.TOKEN_INCREMENT)){
            return new PostfixExpr(var, this.previous, line, this.getCurrentFile());
        }
        else if(matchType(TokenType.TOKEN_LEFT_PAREN)){
            return this.parseCall(var);
        }else if(canAssign && matchType(TokenType.TOKEN_EQUAL)){
            return new AssignExpr(new VarExpr(var, line, this.getCurrentFile()), parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT), line, this.getCurrentFile());
        }else if (canAssign && matchAugmented()){
            return new AugmentedExpr(this.previous, new VarExpr(var, line, this.getCurrentFile()), parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT), line, this.getCurrentFile());
        }
        return new VarExpr(var, line, this.getCurrentFile());
    }
    private Expr logical(Expr expr, boolean canAssign) throws CompileException{
        Token op = this.previous;
        int line = this.previous.line;
        Expr newExpr = parseExpr(this.getEmptyExpr(line), Precedence.AND);
        return new LogicalExpr(expr, newExpr, op, line, this.getCurrentFile());
    }

    private Stmt array(DataType type, String name) throws CompileException{
        int line = this.previous.line;
        List<Expr> items = new ArrayList<>();
        if(!matchType(TokenType.TOKEN_RIGHT_BRACKET)){
            while(true){
                items.add(parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT));
                if(!matchType(TokenType.TOKEN_COMMA)){
                    consume(TokenType.TOKEN_RIGHT_BRACKET, String.format("Unexpected token %s, expected ']' or ','", this.current));
                    break;
                }

            }
        }
       return new ArrayStmt(type, name, items, line, this.getCurrentFile());
    }

    private Expr index(Expr expr, boolean canAssign) throws CompileException{
        int line = this.previous.line;
        Expr out = new IndexExpr(expr, parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT), line, this.getCurrentFile());
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
        this.parseFunctions.put(TokenType.TOKEN_AUGMENTED_XOR, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_AUGMENTED_OR, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_AUGMENTED_AND, new ParseFunction(null, null, Precedence.NONE));
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

    public Parser(Scanner scanner, List<String> included, Map<String, Struct> structs, String fileName){
        this.included = included;
        this.stmts = new ArrayList<>();
        this.defined = new HashMap<>();
        this.extern = new ArrayList<>();
        this.structs = structs;
        this.scanner = scanner;
        this.fileName = fileName;
        this.current = null;
        this.previous = null;


        this.parseFunctions = new EnumMap<>(TokenType.class);
        this.buildParseFunctionMap();
    }
}
