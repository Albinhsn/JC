package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class BinaryExpr extends Expr{

    public Expr left;
    public Expr right;
    public Token op;

    @Override
    public String toString() {
        return String.format("(%s %s %s)", left, op.literal, right);
    }



    public BinaryExpr(Expr left, Token op, Expr right, int line){
        super(line);
        this.left = left;
        this.op = op;
        this.right = right;
    }

    private void checkValidBitwise(Symbol symbol) throws InvalidOperation {
        switch(symbol.type.type){
            case BYTE_POINTER:{}
            case INT_POINTER:{}
            case FLOAT_POINTER:{}
            case STRUCT_POINTER:{}
            case FLOAT :{}
            case STRUCT:{
                throw new InvalidOperation(String.format("Can't do bitwise operation on type '%s' on line %d", symbol.type.name, op.line));
            }
        }
    }

    private QuadList bitwise(SymbolTable symbolTable) throws InvalidOperation, CompileException, UnexpectedTokenException, UnknownSymbolException {
        QuadList l = left.compile(symbolTable);
        Symbol lResult = l.getLastResult();
        l.addQuad(QuadOp.PUSH, null, null, null);
        QuadList r = right.compile(symbolTable);
        Symbol rResult = r.getLastResult();

        l.concat(r);
        l.addQuad(QuadOp.MOV_REG_CA, null, null, null);
        l.addQuad(QuadOp.POP, null, null, lResult);

        checkValidBitwise(lResult);
        checkValidBitwise(rResult);

        if(lResult.type.type == DataTypes.INT || rResult.type.type == DataTypes.INT){
            l.addQuad(QuadOp.fromToken(op), lResult, rResult, Compiler.generateSymbol(DataType.getInt()));
        }else{
            l.addQuad(QuadOp.fromToken(op), lResult, rResult, Compiler.generateSymbol(DataType.getByte()));
        }
        return l;
    }
    private QuadList arithmetic(SymbolTable symbolTable) throws InvalidOperation, CompileException, UnexpectedTokenException, UnknownSymbolException {

        QuadList l = left.compile(symbolTable);
        Symbol lResult = l.getLastResult();
        DataTypes lType = lResult.type.type;

        QuadList r = right.compile(symbolTable);
        Symbol rResult = r.getLastResult();
        DataTypes rType = rResult.type.type;

        if(lResult.type.type == DataTypes.STRUCT || rResult.type.type == DataTypes.STRUCT){
            throw new InvalidOperation(String.format("Can't do operation '%s' on struct on line %d", op.literal, op.line));
        }

        DataType resultType;
        QuadOp quadOp = QuadOp.fromToken(op);

        switch(lType){
            case BYTE:{
                if(rType == DataTypes.BYTE){
                    resultType = lResult.type;
                }else{
                    resultType = rResult.type;
                }
                break;
            }
            case INT:{
                if(rType.isPointer()){
                    resultType = rResult.type;
                }else if(rType == DataTypes.FLOAT){
                    resultType = rResult.type;
                    quadOp = quadOp.convertToFloat();
                }
                else {
                    resultType = lResult.type;
                }
                break;
            }
            case FLOAT:{
                if(rType.isPointer()){
                    throw new InvalidOperation(String.format("Can't do operation '%s' on pointer with type %s on line %d", op.literal, lResult.type.name, op.line));
                }
                quadOp = quadOp.convertToFloat();
                resultType = DataType.getFloat();
                break;
            }
            case INT_POINTER:{}
            case BYTE_POINTER:{}
            case STRUCT_POINTER:{}
            case FLOAT_POINTER:{
                if(rType != DataTypes.INT && rType != DataTypes.BYTE){
                    throw new InvalidOperation(String.format("Can't do operation '%s' on pointer with type %s on line %d", op.literal, rResult.type.name, op.line));
                }
                resultType = lResult.type;
                break;
            }
            default:{
                throw new InvalidOperation(String.format("Can't do operation on type %s on line %d", lResult.type.name, op.line));
            }
        }
        l.concat(r);
        l.addQuad(quadOp, lResult, rResult, Compiler.generateSymbol(resultType));
        return l;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException {
        switch(op.type){
            // These only work on everything except string
            case TOKEN_PLUS : {}
            case TOKEN_MINUS : {}
            case TOKEN_SLASH: {}
            case TOKEN_MOD: {}
            case TOKEN_STAR: {
                return this.arithmetic(symbolTable);
            }

            // These only work on pointers, int and byte
            case TOKEN_AND_BIT:{}
            case TOKEN_OR_BIT: {}
            case TOKEN_XOR : {}
            case TOKEN_SHIFT_LEFT: {}
            case TOKEN_SHIFT_RIGHT: {
                return this.bitwise(symbolTable);
            }
        }
        throw new InvalidOperation(String.format("Can't do binary op with '%s'", op.literal));
    }
}
