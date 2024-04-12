package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    final Scanner scanner;
    Token current;
    Token previous;

    final List<Stmt> stmts;

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

    private TokenType currentToken(){
        return this.current.type;
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
        while(currentToken() != TokenType.TOKEN_RIGHT_BRACE){
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
    private boolean matchBinarOpType() throws IllegalCharacterException, UnterminatedStringException {
        switch(current.type){
            case TOKEN_BANG_EQUAL:{}
            case TOKEN_EQUAL_EQUAL:{}
            case TOKEN_GREATER_EQUAL:{}
            case TOKEN_LESS_EQUAL:{}
            case TOKEN_PLUS:{}
            case TOKEN_MINUS:{}
            case TOKEN_STAR:{}
            case TOKEN_SLASH:{
                advance();
                return true;
            }
        }
        return false;
    }
    private Expr parseExpr(Expr expr) throws IllegalCharacterException, UnterminatedStringException, UnexpectedTokenException {
        // grouped expr
        if(matchType(TokenType.TOKEN_LEFT_PAREN)){
            Expr value = parseExpr(new Expr());
            consume(TokenType.TOKEN_RIGHT_PAREN, "Expected ')' after grouped expression");
            return expr.add(new GroupedExpr(value));
        }
        //  *foo = 5;
        // binary expr
        if(matchBinarOpType()){
            TokenType op = this.previous.type;

        }
        // unaryOp
        // logical expr
        // literal expr
        if (matchType(TokenType.TOKEN_IDENTIFIER)) {
            // function call
            // assignment
            // augmented expr
            //  foo = 5;
            //  foo += 5;
            //  foo.bar = 2;
            //  foo->bar = 2;
        }
        return null;
    }

    private Stmt expressionStmt() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        return new ExprStmt(parseExpr(new Expr()));
    }

    private Stmt returnStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        return new ReturnStmt(parseExpr(new Expr()));
    }
    private Stmt forStatement(){
        return null;
    }

    private Stmt whileStatement(){
        return null;
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
    private Stmt variableStatement() throws UnexpectedTokenException, IllegalCharacterException, UnterminatedStringException {
        VariableType type = parseVariableType();
        if(!matchType(TokenType.TOKEN_IDENTIFIER)){
            error(String.format("Expected identifier after variable type but got %s", this.current.type));
        }
        String name = this.previous.literal;

        // Either it's '=' or '(' for variable and function respectively
        if(matchType(TokenType.TOKEN_EQUAL)){
            Expr value = parseExpr();
            return new VariableStmt(type, name,value);
        }else if(matchType(TokenType.TOKEN_LEFT_PAREN)){
            //parse args

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
    private Stmt ifStatement(){
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
            return variableStatement();
        }else if(matchType(TokenType.TOKEN_IF)){
            return ifStatement();
        }else if(matchType(TokenType.TOKEN_RETURN)){
            return returnStatement();
        }else{
            return this.expressionStmt();
        }
    }


    public void parse(){
        try{
            do{
                advance();
                parseStmt();
            }while(!matchType(TokenType.TOKEN_EOF));
        }catch(IllegalCharacterException | UnterminatedStringException | UnexpectedTokenException e){
            System.out.println(e.getMessage());
        }
    }

    public Parser(Scanner scanner){
        this.stmts = new ArrayList<>();
        this.scanner = scanner;
        this.current = null;
        this.previous = null;
    }
}
