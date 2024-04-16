package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class BinaryExpr implements Expr{

    public Expr left;
    public Expr right;
    public Token op;

    @Override
    public String toString() {
        return String.format("(%s %s %s)", left, op.literal, right);
    }



    public BinaryExpr(Expr left, Token op, Expr right){
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

    private Quad bitwise(Symbol l, Symbol r) throws InvalidOperation, CompileException {
        checkValidBitwise(l);
        checkValidBitwise(r);

        if(l.type.type == DataTypes.INT || r.type.type == DataTypes.INT){
            return new Quad(QuadOp.fromToken(op), l, r, Compiler.generateSymbol(DataType.getInt()));
        }
        return new Quad(QuadOp.fromToken(op), l, r, Compiler.generateSymbol(DataType.getByte()));
    }
    private Quad arithmetic(Symbol l, Symbol r) throws InvalidOperation, CompileException {
        DataType resultType;
        QuadOp quadOp = QuadOp.fromToken(op);
        if(l.type.type == DataTypes.STRUCT || r.type.type == DataTypes.STRUCT){
            throw new InvalidOperation(String.format("Can't do operation '%s' on struct on line %d", op.literal, op.line));
        }

        switch(l.type.type){
            case BYTE:{
                if(r.type.type == DataTypes.BYTE){
                    resultType = l.type;
                }else{
                    resultType = r.type;
                }
                break;
            }
            case INT:{
                if(r.type.type.isPointer()){
                    resultType = r.type;
                }else if(r.type.type == DataTypes.FLOAT){
                    resultType = r.type;
                    quadOp = quadOp.convertToFloat();
                }
                else {
                    resultType = l.type;
                }
                break;
            }
            case FLOAT:{
                if(r.type.type.isPointer()){
                    throw new InvalidOperation(String.format("Can't do operation '%s' on pointer with type %s on line %d", op.literal, l.type.name, op.line));
                }
                quadOp = quadOp.convertToFloat();
                resultType = DataType.getFloat();
                break;
            }
            case INT_POINTER:{}
            case BYTE_POINTER:{}
            case STRUCT_POINTER:{}
            case FLOAT_POINTER:{
                DataTypes rType = r.type.type;
                if(rType != DataTypes.INT && rType != DataTypes.BYTE){
                    throw new InvalidOperation(String.format("Can't do operation '%s' on pointer with type %s on line %d", op.literal, r.type.name, op.line));
                }
                resultType = l.type;
                break;
            }
            default:{
                throw new InvalidOperation(String.format("Can't do operation on type %s on line %d", l.type.name, op.line));
            }
        }
        return new Quad(quadOp, l, r, Compiler.generateSymbol(resultType));
    }

    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException {
        List<Quad> l = left.compile(symbolTable);
        Symbol lResult = Quad.getLastResult(l);
        l.add(new Quad(QuadOp.PUSH, null, null, null));
        List<Quad> r = right.compile(symbolTable);
        Symbol rResult = Quad.getLastResult(r);

        l.addAll(r);
        l.add(new Quad(QuadOp.MOV_REG_CA, null, null, null));
        l.add(new Quad(QuadOp.POP, null, null, lResult));


        switch(op.type){
            // These only work on everything except string
            case TOKEN_PLUS : {}
            case TOKEN_MINUS : {}
            case TOKEN_SLASH: {}
            case TOKEN_MOD: {}
            case TOKEN_STAR: {
                l.add(this.arithmetic(lResult,rResult));
                break;
            }

            // These only work on pointers, int and byte
            case TOKEN_AND_BIT:{}
            case TOKEN_OR_BIT: {}
            case TOKEN_XOR : {}
            case TOKEN_SHIFT_LEFT: {}
            case TOKEN_SHIFT_RIGHT: {
                l.add(this.bitwise(lResult, rResult));
                break;
            }
            default:{ throw new InvalidOperation(String.format("Can't do binary op '%s' with types of %s and %s", op.literal, lResult.type.name, rResult.type.name));}
        }

        return l;
    }
}
