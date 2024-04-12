package se.liu.albhe576.project;

import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.util.*;

public class Parser {
    @FunctionalInterface
    private interface InfixRule{
        public Expr infix(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException;
    }
    @FunctionalInterface
    private interface PrefixRule{
        public Expr prefix(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException;
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
    final Scanner scanner;
    private Token current;
    private Token previous;

    private final List<Stmt> stmts;


    private void advance() throws IllegalCharacterException, UnterminatedStringException {
        previous = current;
        current = scanner.parseToken();
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
            case TOKEN_BYTE:{
                return true;
            }
        }
        return false;
    }

    private VariableType parseVariableType() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        if(matchType(TokenType.TOKEN_INT)){
            if(matchType(TokenType.TOKEN_STAR)){
                return VariableType.INT_POINTER;
            }
            return VariableType.INT;
        }else if(matchType(TokenType.TOKEN_FLOAT)){
            if(matchType(TokenType.TOKEN_STAR)){
                return VariableType.FLOAT_POINTER;
            }
            return VariableType.FLOAT;
        }else if(matchType(TokenType.TOKEN_BYTE)){
            if(matchType(TokenType.TOKEN_STAR)){
                return VariableType.BYTE_POINTER;
            }
            return VariableType.BYTE;
        }
        throw new UnexpectedTokenException("");
    }

    private Stmt structDeclaration() throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        if(!matchType(TokenType.TOKEN_IDENTIFIER)){
            error(String.format("Expected identifier but got %s", this.current.type));
        }
        String name = this.previous.literal;
        if(!matchType(TokenType.TOKEN_LEFT_BRACE)){
            error(String.format("Expected left brace but got %s", this.current.type));
        }

        List<StructField> fields = new ArrayList<>();
        while(this.current.type != TokenType.TOKEN_RIGHT_BRACE){
            VariableType fieldType = parseVariableType();

            if(!matchType(TokenType.TOKEN_IDENTIFIER)){
                throw new UnexpectedTokenException(String.format("Expected identifier but got %s", this.current.type));
            }
            String fieldName = this.previous.literal;

            if(!matchType(TokenType.TOKEN_SEMICOLON)){
                throw new UnexpectedTokenException(String.format("Expected semicolon after struct field but got %s", this.current.type));
            }
            fields.add(new StructField(fieldName, fieldType));
        }
        return new StructStmt(name, fields);
    }

    private void consume(TokenType type, String msg) throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        if(!matchType(type)){
            throw new UnexpectedTokenException(msg);
        }
    }
    private Expr literal(Expr expr, boolean canAssign){
        return expr.literal(this.previous);
    }


    private Expr parseExpr(Expr expr, Precedence precedence) throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {

        System.out.printf("Parsing with %s %s\n", precedence, this.current.literal);
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

    private Stmt expressionStmt() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        return new ExprStmt(parseExpr(new Expr(), Precedence.ASSIGNMENT));
    }

    private Stmt returnStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        Stmt out = new ReturnStmt(parseExpr(new Expr(), Precedence.ASSIGNMENT));
        consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after return stmt %s", out));
        return out;
    }
    private Stmt forStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected '(' after for");
        Stmt init = parseStmt();
        Stmt condition = expressionStmt();
        consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after condition stmt %s", condition));
        Stmt update = expressionStmt();
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after loop control variables");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' in for loop");

        List<Stmt> body = new ArrayList<>();
        do{
            body.add(parseStmt());
        }while(!matchType(TokenType.TOKEN_RIGHT_BRACE));

        return new ForStmt(init, condition, update, body);

    }

    private Stmt whileStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        consume(TokenType.TOKEN_LEFT_PAREN, "expected '(' after while");
        Expr condition = parseExpr(new Expr(), Precedence.ASSIGNMENT);
        consume(TokenType.TOKEN_RIGHT_PAREN, "expected ')' after while");
        consume(TokenType.TOKEN_LEFT_BRACE, "Expected '{' after while condition");

        List<Stmt> body = parseBody();
        return new WhileStmt(condition, body);
    }

    private List<Stmt> parseBody() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        List<Stmt> body = new ArrayList<>();
        do{
            body.add(parseStmt());
        }while(!matchType(TokenType.TOKEN_RIGHT_BRACE));
        return body;
    }
    private List<StructField> parseArguments() throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        List<StructField> args = new ArrayList<>();
        do{
            VariableType type = parseVariableType();
            if(!matchType(TokenType.TOKEN_IDENTIFIER)){
                throw new UnexpectedTokenException(String.format("Expected identifier as argument but got %s", this.current.type));
            }
            String name = this.previous.literal;
            args.add(new StructField(name, type));

        }while(matchType(TokenType.TOKEN_COMMA));

        return args;
    }
    private Stmt variableDeclaration() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        VariableType type = parseVariableType();
        if(!matchType(TokenType.TOKEN_IDENTIFIER)){
            error(String.format("Expected identifier after variable type but got %s", this.current.type));
        }
        String name = this.previous.literal;

        // Either it's '=' or '(' for variable and function respectively
        if(matchType(TokenType.TOKEN_EQUAL)){
            Expr value = parseExpr(new Expr(), Precedence.ASSIGNMENT);
            consume(TokenType.TOKEN_SEMICOLON, String.format("expected semicolon after assign expr but got %s", this.current.type));
            return new VariableStmt(type, name,value);
        }else if(matchType(TokenType.TOKEN_LEFT_PAREN)){
            List<StructField> args = this.parseArguments();
            if(!matchType(TokenType.TOKEN_RIGHT_PAREN)){
                throw new UnexpectedTokenException(String.format("Expected right paren after arguments but got %s", this.current.type));
            }
            if(!matchType(TokenType.TOKEN_LEFT_BRACE)){
                throw new UnexpectedTokenException(String.format("Expected { to start function body but got %s", this.current.type));
            }

            List<Stmt> body = this.parseBody();
            return new FunctionStmt(type, name,args, body);
        }else{
            throw new UnexpectedTokenException(String.format("Expected '=' or '(' after variable but got %s", this.current.type));
        }

    }
    private Stmt ifStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        consume(TokenType.TOKEN_LEFT_PAREN, "Expected '(' after if");
        Expr condition = parseExpr(new Expr(), Precedence.ASSIGNMENT);

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

        return new IfStmt(condition, ifBody, elseBody);
    }
    private Stmt variableStatement() throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        Token var = this.current;
        advance();
        if(matchType(TokenType.TOKEN_EQUAL)){
            Expr assignValue = parseExpr(new Expr(), Precedence.ASSIGNMENT);
            return new ExprStmt(new AssignExpr(var, assignValue));
        }else if(matchAugmented()){
            return new ExprStmt(new AugmentedExpr(this.previous, var, parseExpr(new Expr(), Precedence.ASSIGNMENT)));
        }
        error("Didn't expect to get here, this won't evaluate to anything and we don't know how to parse it yet :)");
        return null;
    }
    private Stmt parseStmt() throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        if(matchType(TokenType.TOKEN_STRUCT)){
            return structDeclaration();
        }else if(matchType(TokenType.TOKEN_FOR)){
            return forStatement();
        }else if(matchType(TokenType.TOKEN_WHILE)){
            return whileStatement();
        }else if(isVariableType()){
            return variableDeclaration();
        }else if(current.type == TokenType.TOKEN_IDENTIFIER){
            Stmt out = variableStatement();
            consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after variable Stmt %s", out));
            return out;
        }else if(matchType(TokenType.TOKEN_IF)){
            return ifStatement();
        }else if(matchType(TokenType.TOKEN_RETURN)){
            return returnStatement();
        }else{
            Stmt out = expressionStmt();
            consume(TokenType.TOKEN_SEMICOLON, String.format("Expected ';' after expression Stmt %s", out));
            return out;
        }
    }


    public List<Stmt> parse(){
        try{
            advance();
            while(!matchType(TokenType.TOKEN_EOF)){
                this.stmts.add(parseStmt());
                advance();
            }
        }catch(IllegalCharacterException | UnterminatedStringException | UnexpectedTokenException e){
            System.out.println(e.getMessage());
        }
        return this.stmts;
    }
    private Expr grouping(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        Expr groupedExpr = parseExpr(new Expr(), Precedence.ASSIGNMENT);
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after grouped expression");
        return expr.grouped(new GroupedExpr(groupedExpr));
    }
    private Expr call(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        List<Expr> args = new ArrayList<>();
        if(this.current.type != TokenType.TOKEN_RIGHT_PAREN){
            do{
                args.add(parseExpr(new Expr(), Precedence.ASSIGNMENT));
            }while(matchType(TokenType.TOKEN_COMMA));
        }
        consume(TokenType.TOKEN_RIGHT_PAREN, "Expect right paren after args");
        return new CallExpr(expr, args);
    }
    private Expr dot(Expr expr, boolean canAssign){
        return null;
    }
    private Expr binary(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        Token op = this.previous;
        ParseFunction rule = this.parseFunctions.get(op.type);
        Expr newExpr = parseExpr(new Expr(), Precedence.values()[rule.precedence.ordinal() + 1]);
        return new BinaryExpr(expr, op, newExpr);
    }
    private Expr unary(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        Token op = this.previous;
        Expr newExpr = parseExpr(new Expr(), Precedence.UNARY);
        return new UnaryExpr(newExpr, op);
    }
    private Expr variable(Expr expr, boolean canAssign) throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        Token var = this.previous;
        if(matchType(TokenType.TOKEN_LEFT_PAREN)){
            return call(new VarExpr(var), false);
        }
        return new VarExpr(var);
    }
    private Expr logical(Expr expr, boolean canAssign) throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        Token op = this.previous;
        Expr newExpr = parseExpr(new Expr(), Precedence.AND);
        return new LogicalExpr(expr, newExpr, op);
    }

    private void buildParseFunctionMap(){
        this.parseFunctions.put(TokenType.TOKEN_LEFT_PAREN, new ParseFunction(this::grouping, this::call, Precedence.CALL));
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
        this.parseFunctions.put(TokenType.TOKEN_STAR, new ParseFunction(this::unary, this::binary, Precedence.FACTOR));
        this.parseFunctions.put(TokenType.TOKEN_BANG, new ParseFunction(this::unary, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_BANG_EQUAL, new ParseFunction(null, this::binary, Precedence.EQUALITY));
        this.parseFunctions.put(TokenType.TOKEN_EQUAL, new ParseFunction(null, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_EQUAL_EQUAL, new ParseFunction(null, this::binary, Precedence.EQUALITY));
        this.parseFunctions.put(TokenType.TOKEN_GREATER, new ParseFunction(null, this::binary, Precedence.COMPARISON));
        this.parseFunctions.put(TokenType.TOKEN_GREATER_EQUAL, new ParseFunction(null, this::binary, Precedence.COMPARISON));
        this.parseFunctions.put(TokenType.TOKEN_LESS, new ParseFunction(null, this::binary, Precedence.COMPARISON));
        this.parseFunctions.put(TokenType.TOKEN_LESS_EQUAL, new ParseFunction(null, this::binary, Precedence.COMPARISON));
        this.parseFunctions.put(TokenType.TOKEN_IDENTIFIER, new ParseFunction(this::variable, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_STRING, new ParseFunction(this::literal, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_INT_LITERAL, new ParseFunction(this::literal, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_FLOAT_LITERAL, new ParseFunction(this::literal, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_AND_LOGICAL, new ParseFunction(null, this::logical, Precedence.AND));
        this.parseFunctions.put(TokenType.TOKEN_AND_BIT, new ParseFunction(null, this::binary, Precedence.BITWISE));
        this.parseFunctions.put(TokenType.TOKEN_OR_LOGICAL, new ParseFunction(null, this::logical, Precedence.OR));
        this.parseFunctions.put(TokenType.TOKEN_OR_BIT, new ParseFunction(null, this::binary, Precedence.BITWISE));
        this.parseFunctions.put(TokenType.TOKEN_SHIFT_LEFT, new ParseFunction(null, this::binary, Precedence.BITWISE));
        this.parseFunctions.put(TokenType.TOKEN_SHIFT_RIGHT, new ParseFunction(null, this::binary, Precedence.BITWISE));
        this.parseFunctions.put(TokenType.TOKEN_TRUE, new ParseFunction(this::literal, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_FALSE, new ParseFunction(this::literal, null, Precedence.NONE));
        this.parseFunctions.put(TokenType.TOKEN_INCREMENT, new ParseFunction(this::unary, this::unary, Precedence.TERM));
        this.parseFunctions.put(TokenType.TOKEN_DECREMENT, new ParseFunction(this::unary, this::unary, Precedence.TERM));
    }

    public Parser(Scanner scanner){
        this.stmts = new ArrayList<>();
        this.scanner = scanner;
        this.current = null;
        this.previous = null;


        this.parseFunctions = new EnumMap<>(TokenType.class);
        this.buildParseFunctionMap();
    }
}
