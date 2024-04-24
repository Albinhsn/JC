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

    private record ParseFunction(PrefixRule prefixRule, InfixRule infixRule, Precedence precedence) { }

    private final EnumMap<TokenType, ParseFunction> parseFunctions = new EnumMap<>(Map.ofEntries(
        Map.entry(TokenType.TOKEN_LEFT_BRACKET, new ParseFunction(null, this::index, Precedence.CALL)),
        Map.entry(TokenType.TOKEN_RIGHT_BRACKET, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_LEFT_PAREN, new ParseFunction(this::grouping, null, Precedence.CALL)),
        Map.entry(TokenType.TOKEN_RIGHT_PAREN, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_LEFT_BRACE, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_RIGHT_BRACE, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_COMMA, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_DOT, new ParseFunction(null, this::dot, Precedence.CALL)),
        Map.entry(TokenType.TOKEN_AUGMENTED_SLASH, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_AUGMENTED_STAR, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_AUGMENTED_MINUS, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_AUGMENTED_PLUS, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_AUGMENTED_XOR, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_AUGMENTED_OR, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_AUGMENTED_AND, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_MINUS, new ParseFunction(this::unary, this::binary, Precedence.TERM)),
        Map.entry(TokenType.TOKEN_PLUS, new ParseFunction(null, this::binary, Precedence.TERM)),
        Map.entry(TokenType.TOKEN_SEMICOLON, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_SLASH, new ParseFunction(null, this::binary, Precedence.FACTOR)),
        Map.entry(TokenType.TOKEN_STAR, new ParseFunction(this::dereference, this::binary, Precedence.FACTOR)),
        Map.entry(TokenType.TOKEN_BANG, new ParseFunction(this::unary, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_BANG_EQUAL, new ParseFunction(null, this::comparison, Precedence.EQUALITY)),
        Map.entry(TokenType.TOKEN_EQUAL, new ParseFunction(null, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_EQUAL_EQUAL, new ParseFunction(null, this::comparison, Precedence.EQUALITY)),
        Map.entry(TokenType.TOKEN_GREATER, new ParseFunction(null, this::comparison, Precedence.COMPARISON)),
        Map.entry(TokenType.TOKEN_GREATER_EQUAL, new ParseFunction(null, this::comparison, Precedence.COMPARISON)),
        Map.entry(TokenType.TOKEN_LESS, new ParseFunction(null, this::comparison, Precedence.COMPARISON)),
        Map.entry(TokenType.TOKEN_LESS_EQUAL, new ParseFunction(null, this::comparison, Precedence.COMPARISON)),
        Map.entry(TokenType.TOKEN_IDENTIFIER, new ParseFunction(this::variable, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_STRING_LITERAL, new ParseFunction(this::literal, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_INT_LITERAL, new ParseFunction(this::literal, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_FLOAT_LITERAL, new ParseFunction(this::literal, null, Precedence.NONE)),
        Map.entry(TokenType.TOKEN_AND_LOGICAL, new ParseFunction(null, this::logical, Precedence.AND)),
        Map.entry(TokenType.TOKEN_AND_BIT, new ParseFunction(this::unary, this::binary, Precedence.BITWISE)),
        Map.entry(TokenType.TOKEN_OR_LOGICAL, new ParseFunction(null, this::logical, Precedence.OR)),
        Map.entry(TokenType.TOKEN_OR_BIT, new ParseFunction(null, this::binary, Precedence.BITWISE)),
        Map.entry(TokenType.TOKEN_XOR, new ParseFunction(null, this::binary, Precedence.BITWISE)),
        Map.entry(TokenType.TOKEN_SHIFT_LEFT, new ParseFunction(null, this::binary, Precedence.BITWISE)),
        Map.entry(TokenType.TOKEN_SHIFT_RIGHT, new ParseFunction(null, this::binary, Precedence.BITWISE)),
        Map.entry(TokenType.TOKEN_INCREMENT, new ParseFunction(this::unary, this::postfix, Precedence.TERM)),
        Map.entry(TokenType.TOKEN_DECREMENT, new ParseFunction(this::unary,this::postfix, Precedence.TERM)),
        Map.entry(TokenType.TOKEN_MOD, new ParseFunction(null, this::binary, Precedence.TERM)),
        Map.entry(TokenType.TOKEN_ELLIPSIS, new ParseFunction(null, null, Precedence.NONE))
    ));
    private final Map<String, Token> defined;
    private final List<String> included;
    private final String fileName;
    private final Map<String, Function> extern;
    private final Map<String, Struct> structs;
    private final Scanner scanner;
    private Token current;
    private Token previous;
    private final List<Stmt> stmts;

    public Map<String, Function> getExtern(){
        return this.extern;
    }
    public Map<String, Token> getDefined(){
        return this.defined;
    }
    public Map<String, Struct> getStructs(){
        return this.structs;
    }

    private void updateCurrent(){
        if(this.current.type() == TokenType.TOKEN_IDENTIFIER && this.defined.containsKey(this.current.literal())){
            Token value = this.defined.get(this.current.literal());
            this.current = new Token(value.type(), this.scanner.getLine(), value.literal());
        }
    }

    private void advance() throws CompileException{
        previous = current;
        current = scanner.parseToken();
        this.updateCurrent();
    }

    private boolean matchType(TokenType type) throws CompileException{
        if(this.current.type() != type){
           return false;
        }
        advance();
        return true;
    }

    private boolean isVariableType() {
        switch(this.current.type()){
            case TOKEN_INT:{}
            case TOKEN_FLOAT:{}
            case TOKEN_STRING:{}
            case TOKEN_BYTE:{}
            case TOKEN_VOID:{
                return true;
            }
        }
        return false;
    }

    private DataType parsePointerType(DataType type) throws CompileException{
        if(this.current.type() == TokenType.TOKEN_STAR){
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

        DataType dataType = this.parseType();
        if(!matchType(TokenType.TOKEN_IDENTIFIER)){
            Compiler.error(String.format("Expected identifier as argument but got %s", this.current.type()), this.current.line(), this.fileName);
        }
        String name = this.previous.literal();
        return new StructField(name, dataType);
    }

    private Expr postfix(Expr expr, boolean canAssign){return new PostfixExpr(expr, this.previous, this.scanner.getLine(), this.fileName);}

    private void structDeclaration() throws CompileException {
        if(!matchType(TokenType.TOKEN_IDENTIFIER)){
            Compiler.error(String.format("Expected identifier but got %s", this.current.type()), this.current.line(), this.fileName);
        }
        Token name = this.previous;
        if(!matchType(TokenType.TOKEN_LEFT_BRACE)){
            Compiler.error(String.format("Expected left brace but got %s", this.current.type()), this.current.line(), this.fileName);
        }

        List<StructField> fields = new ArrayList<>();
        while(!matchType(TokenType.TOKEN_RIGHT_BRACE)){
            fields.add(this.parseStructField());
            if(!matchType(TokenType.TOKEN_SEMICOLON)){
                Compiler.error(String.format("Expected semicolon after struct field but got %s", this.current.type()), this.current.line(), this.fileName);
            }
        }
        if(this.structs.containsKey(name.literal())){
            Struct struct = this.structs.get(name.literal());
            Compiler.error(String.format("Can't declare struct '%s' which is already declared from file %s", name.literal(), struct.getFilename()), this.current.line(), this.fileName);
        }
        this.structs.put(name.literal(), new Struct(fields, this.fileName));
    }

    private void consume(TokenType type, String msg) throws CompileException{
        if(!matchType(type)){
            Compiler.error(msg, this.current.line(), this.fileName);
        }
    }
    private Expr literal(Expr expr, boolean canAssign) {return new LiteralExpr(this.previous, this.previous.line(), this.fileName);}

    private Expr parseExpr(Expr expr, Precedence precedence) throws CompileException{

        advance();
        ParseFunction prefix = this.parseFunctions.getOrDefault(this.previous.type(), null);
        if(prefix == null || prefix.prefixRule == null){
            Compiler.error(String.format("Expected expression but got %s %s", this.previous.literal(), precedence), this.current.line(), this.fileName);
        }
        assert prefix != null;

        final int precedenceOrdinal = precedence.ordinal();
        final boolean canAssign =  precedenceOrdinal <= Precedence.ASSIGNMENT.ordinal();
        if(prefix.prefixRule == null){
            Compiler.error(String.format("Can't parse prefix of %s", this.previous.literal()), this.previous.line(), this.fileName);
        }
        assert prefix.prefixRule != null;
        expr = prefix.prefixRule.prefix(expr, canAssign);

        ParseFunction currentRule = this.parseFunctions.get(this.current.type());
        while(precedenceOrdinal <= currentRule.precedence.ordinal()){
            advance();
            expr = currentRule.infixRule.infix(expr, canAssign);
            currentRule = this.parseFunctions.get(this.current.type());
        }

        if(canAssign && matchType(TokenType.TOKEN_EQUAL)){
            Compiler.error("Invalid assignment target", this.scanner.getLine(), this.fileName);
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

        int line = this.current.line();
        Expr expr = parseExpr(this.getEmptyExpr(line), Precedence.OR);
        if(matchType(TokenType.TOKEN_EQUAL)) {
            return new AssignStmt(expr, parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT), line, this.fileName);
        }
        else if(matchAugmented()){
            return new AssignStmt(expr, new BinaryExpr(expr, this.previous,parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT), line, this.fileName), line, this.fileName);
        }
        return new ExprStmt(expr, line, this.fileName);
    }

    private Stmt returnStatement() throws CompileException{
        this.advance();
        if(matchType(TokenType.TOKEN_SEMICOLON)){
            return new ReturnStmt(null, this.previous.line(), this.fileName);
        }
        Stmt out = new ReturnStmt(parseExpr(this.getEmptyExpr(this.current.line()), Precedence.ASSIGNMENT), this.previous.line(), this.fileName);
        consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after return stmt %s", out));
        return out;
    }

    private Stmt forStatement() throws CompileException {
        this.advance();
        int line = this.previous.line();
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected '(' after for");

        Stmt init = parseStmt();
        Stmt condition = expressionStatement();

        consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after condition stmt %s", condition));
        Stmt update = expressionStatement();
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after loop control variables");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' in for loop");

        // Body consumes }
        List<Stmt> body = this.parseBody();

        return new ForStmt(init, condition, update, body, line, this.fileName);

    }

    private Stmt whileStatement() throws CompileException {
        this.advance();
        int line = this.previous.line();
        consume(TokenType.TOKEN_LEFT_PAREN, "expected '(' after while");
        Expr condition = parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT);
        consume(TokenType.TOKEN_RIGHT_PAREN, "expected ')' after while");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after while condition");

        List<Stmt> body = parseBody();
        return new WhileStmt(condition, body, line, this.fileName);
    }


    private List<Stmt> parseBody() throws CompileException {
        List<Stmt> body = new ArrayList<>();
        while(!matchType(TokenType.TOKEN_RIGHT_BRACE)) {
            if(matchType(TokenType.TOKEN_SEMICOLON)){
                System.out.printf("Extra semicolon in file %s at line %d?", fileName, this.scanner.getLine());
            }else{
                body.add(parseStmt());
            }
        }
        return body;
    }
    private List<StructField> parseArguments() throws CompileException {
        List<StructField> args = new ArrayList<>();
        if(this.current.type() != TokenType.TOKEN_RIGHT_PAREN){
            do{
                args.add(this.parseStructField());
            }while(matchType(TokenType.TOKEN_COMMA));
        }
        return args;
    }

    private Stmt function(DataType type, String name, int line) throws CompileException {
        List<StructField> args = this.parseArguments();

        consume(TokenType.TOKEN_RIGHT_PAREN, String.format("Expected right paren after arguments but got %s", this.current.type()));
        consume(TokenType.TOKEN_LEFT_BRACE, String.format("Expected { to start function body but got %s", this.current.type()));

        List<Stmt> body = this.parseBody();
        return new FunctionStmt(type, name,args, body, line, this.fileName);
    }

    private Stmt array(String name, DataType type, int line) throws CompileException {
        ArrayDataType arrayType = ArrayDataType.fromItemType(type);
        consume(TokenType.TOKEN_INT_LITERAL, String.format("Expected array size in form of int literal in array declaration, got %s", this.current.literal()));

        // Probably no reason that you should allow a[0xF] but why not
        int size = Integer.decode(this.previous.literal());
        consume(TokenType.TOKEN_RIGHT_BRACKET, String.format("Expected ']' after array size in array declaration, got %s", this.current.literal()));

        List<Expr> items = new ArrayList<>();
        if(matchType(TokenType.TOKEN_EQUAL)){
            consume(TokenType.TOKEN_LEFT_BRACKET, "Expected '[' after = in array declaration");
            items = parseArrayItems(size);
        }

        consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after array declaration, not %s?", this.current.literal()));
        return new ArrayStmt(arrayType, name,items, size, line, fileName);

    }

    private Stmt variableDeclaration(DataType type) throws CompileException {
        int line = this.current.line();

        consume(TokenType.TOKEN_IDENTIFIER, String.format("Expected identifier after variable type but got %s", this.current.type()));
        String name = this.previous.literal();

        advance();
        switch(this.previous.type()){
            case TOKEN_EQUAL->{
                Expr value = parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT);
                consume(TokenType.TOKEN_SEMICOLON, String.format("expected semicolon after assign expr but got %s", this.current.type()));
                return new VariableStmt(type, name,value, line, this.fileName);
            }
            case TOKEN_LEFT_PAREN -> {return this.function(type, name, line);}
            case TOKEN_SEMICOLON -> {return new VariableStmt(type, name, null, line, this.fileName);}
            default -> {
                // Do this rather than another case and then throw exception or call Compiler.error
                // Since java won't compile unless we return something :)
                if(this.previous.type() != TokenType.TOKEN_LEFT_BRACKET){
                    Compiler.error("Expected '[', '=', '(' or ';' after variable name", this.previous.line(), this.fileName);
                }
                return this.array(name, type, line);
            }
        }
    }
    private Stmt ifStatement() throws CompileException {
        this.advance();
        int line = this.previous.line();
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected '(' after if");
        Expr condition = parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT);

        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after if condition");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after if condition");

        List<Stmt> ifBody = parseBody();
        List<Stmt> elseBody = new ArrayList<>();

        if(matchType(TokenType.TOKEN_ELSE)){
            consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after if condition");
            elseBody = parseBody();
        }

        return new IfStmt(condition, ifBody, elseBody, line, this.fileName);
    }
    private Stmt parseStmt() throws CompileException {
        switch(this.current.type()){
            case TOKEN_FOR -> {return forStatement();}
            case TOKEN_WHILE -> {return whileStatement();}
            case TOKEN_IF-> {return ifStatement();}
            case TOKEN_RETURN-> {return returnStatement();}
            default -> {
                if(isVariableType() || this.structs.containsKey(this.current.literal())){
                    DataType type = parseType();
                    return variableDeclaration(type);
                }

                Stmt out = expressionStatement();
                consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after expression Stmt %s", out));
                return out;
            }

        }
    }


    private void parseInclude() throws CompileException {
        consume(TokenType.TOKEN_STRING_LITERAL, "Expected string after include?");
        String fileName = this.previous.literal();

        if(this.included.stream().anyMatch(i -> i.equals(fileName))){
            System.out.printf("already included %s\n%n", fileName);
            return;
        }

        this.included.add(fileName);
        String s = null;
        try{
            s = Files.readString(Path.of(fileName));
        }catch(IOException e){
            Compiler.error(String.format("Failed to read from include %s", fileName), this.current.line(), this.fileName);
        }

        Parser includeParser = new Parser(new Scanner(s), this.included, this.structs, fileName);
        List<Stmt> included = includeParser.parse();

        this.extern.putAll(includeParser.getExtern());
        this.defined.putAll(includeParser.getDefined());
        this.structs.putAll(includeParser.structs);
        this.stmts.addAll(included);
    }

    private void parseDefine() throws CompileException {
        consume(TokenType.TOKEN_IDENTIFIER, "expected identifier after define");
        this.defined.put(this.previous.literal(), this.current);
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

        consume(TokenType.TOKEN_IDENTIFIER, "Expected identifier for function name after #extern");
        String funcName = this.previous.literal();
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected ( for function args after function name in extern");
        if(matchType(TokenType.TOKEN_ELLIPSIS)){
            this.extern.put(funcName, new Function(type, null, this.fileName, this.previous.line()));
        }else{
            this.extern.put(funcName, new Function(this.parseArguments(), type, null, this.fileName, this.previous.line(), true));
        }
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ) after function args in extern");

    }

    public List<Stmt> parse() throws CompileException {
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
            else if(matchType(TokenType.TOKEN_SEMICOLON)){
                // ToDo loggin
                System.out.printf("Extra semicolon in file %s at line %d?", fileName, this.scanner.getLine());
            }else{
                this.stmts.add(parseStmt());
            }
        }
        return this.stmts;
    }
    private Expr grouping(Expr expr, boolean canAssign) throws CompileException{
        int line = this.previous.line();
        Expr groupedExpr = parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT);
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after grouped expression");
        return new GroupedExpr(groupedExpr, line, this.fileName);
    }
    private Expr dot(Expr expr, boolean canAssign) throws CompileException{
        int line = this.previous.line();
        consume(TokenType.TOKEN_IDENTIFIER, "Expected identifier after dot");
        Token var = this.previous;

        return new DotExpr(expr, var, line, this.fileName);
    }

    private Expr parseRightSideOfBinary(Token op, int line) throws CompileException{
        ParseFunction rule = this.parseFunctions.get(op.type());
        return parseExpr(this.getEmptyExpr(line), Precedence.values()[rule.precedence.ordinal() + 1]);
    }
    private Expr comparison(Expr left, boolean canAssign) throws CompileException{
        Token op = this.previous;
        int line = this.previous.line();
        return new ComparisonExpr(left, parseRightSideOfBinary(op, line), op, line, this.fileName);
    }

    private Expr binary(Expr expr, boolean canAssign) throws CompileException{
        Token op = this.previous;
        int line = this.previous.line();
        return new BinaryExpr(expr, op, parseRightSideOfBinary(op, line), line, this.fileName);
    }
    private Expr dereference(Expr expr, boolean canAssign) throws CompileException{
        Token op = this.previous;
        int line = this.previous.line();
        UnaryExpr headExpr = new UnaryExpr(this.getEmptyExpr(line), op, line, this.fileName);
        UnaryExpr unaryExpr = headExpr;
        while(matchType(TokenType.TOKEN_STAR)){
            unaryExpr.expr = new UnaryExpr(this.getEmptyExpr(line), op, line, this.fileName);
            unaryExpr = (UnaryExpr) unaryExpr.expr;
        }
        Precedence precedence = canAssign ? Precedence.ASSIGNMENT : Precedence.OR;
        unaryExpr.expr = parseExpr(this.getEmptyExpr(line), precedence);
        return headExpr;
    }

    private Expr unary(Expr expr, boolean canAssign) throws CompileException{
        Token op = this.previous;
        int line = this.previous.line();
        Expr newExpr = parseExpr(this.getEmptyExpr(line), Precedence.UNARY);
        return new UnaryExpr(newExpr, op, line, this.fileName);
    }
    private Expr parseCall(Token var) throws CompileException {
        int line = this.previous.line();
        List<Expr> args = new ArrayList<>();
        if(this.current.type() != TokenType.TOKEN_RIGHT_PAREN){
            do{
                args.add(parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT));
            }while(matchType(TokenType.TOKEN_COMMA));
        }
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expect right paren after args");
        return new CallExpr(var, args, line, this.fileName);
    }

    private Expr variable(Expr expr, boolean canAssign) throws CompileException{
        Token var = this.previous;
        int line = this.previous.line();
        if(matchType(TokenType.TOKEN_LEFT_PAREN)){
            return this.parseCall(var);
        }
        return new VarExpr(var, line, this.fileName);
    }
    private Expr logical(Expr expr, boolean canAssign) throws CompileException{
        Token op = this.previous;
        int line = this.previous.line();
        Expr newExpr = parseExpr(this.getEmptyExpr(line), Precedence.AND);
        return new LogicalExpr(expr, newExpr, op, line, this.fileName);
    }

    private List<Expr> parseArrayItems(int size) throws CompileException{
        int line = this.previous.line();
        List<Expr> items = new ArrayList<>();
        if(!matchType(TokenType.TOKEN_RIGHT_BRACKET)){
            items.add(parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT));
            while(matchType(TokenType.TOKEN_COMMA)){
                items.add(parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT));
            }
            consume(TokenType.TOKEN_RIGHT_BRACKET, String.format("Unexpected token %s, expected ']' or ','", this.current));
        }
        if(size < items.size()){
            Compiler.error(String.format("Array declaration has excess element! Expected %d got %d", size, items.size()), this.current.line(), this.fileName);
        }

       return items;
    }

    private Expr index(Expr expr, boolean canAssign) throws CompileException{
        int line = this.previous.line();
        Expr out = new IndexExpr(expr, parseExpr(this.getEmptyExpr(line), Precedence.ASSIGNMENT), line, this.fileName);
        consume(TokenType.TOKEN_RIGHT_BRACKET, "Expected ']' after index");
        return out;
    }

    private Expr getEmptyExpr(int line){return new Expr(line, this.fileName);}

    public Parser(Scanner scanner, List<String> included, Map<String, Struct> structs, String fileName){
        this.included = included;
        this.stmts = new ArrayList<>();
        this.defined = new HashMap<>();
        this.extern = new HashMap<>();
        this.structs = structs;
        this.scanner = scanner;
        this.fileName = fileName;
        this.current = null;
        this.previous = null;

    }
}
