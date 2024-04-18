package se.liu.albhe576.project;

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


    private QuadList bitwise(SymbolTable symbolTable) throws InvalidOperation, CompileException, UnexpectedTokenException, UnknownSymbolException {
        QuadList l = left.compile(symbolTable);
        Symbol lResult = l.getLastResult();
        l.addQuad(QuadOp.PUSH, null, null, null);
        QuadList r = right.compile(symbolTable);
        Symbol rResult = r.getLastResult();

        l.concat(r);
        l.addQuad(QuadOp.MOV_REG_CA, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));
        l.addQuad(QuadOp.POP, null, null, lResult);

        if(lResult.type.type == DataTypes.INT && rResult.type.type == DataTypes.INT){
            l.addQuad(QuadOp.fromToken(op), lResult, rResult, Compiler.generateSymbol(DataType.getInt()));
            return l;
        }
        throw new InvalidOperation(String.format("Can only do bitwise on ints, line %d", op.line));

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

        DataType resultType = lResult.type;
        QuadOp quadOp = QuadOp.fromToken(op);

        if(lType == DataTypes.FLOAT || rType == DataTypes.FLOAT){
            quadOp = quadOp.convertToFloat();
        }

        if((lType.isPointer() && rType.isInteger()) || (lType.isInteger() && rType.isPointer())){
            resultType = lType.isPointer() ? lResult.type : rResult.type;
            if(lType.isPointer()){
                int structSize = symbolTable.getStructSize(lResult.type);
                r.addQuad(QuadOp.MOV_REG_CA, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));
                r.addQuad(QuadOp.LOAD_IMM,Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(structSize)), null, Compiler.generateSymbol(DataType.getInt()));
                r.addQuad(QuadOp.MUL, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));
            }else{
                int structSize = symbolTable.getStructSize(rResult.type);
                l.addQuad(QuadOp.MOV_REG_CA, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));
                l.addQuad(QuadOp.LOAD_IMM,Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(structSize)), null, Compiler.generateSymbol(DataType.getInt()));
                l.addQuad(QuadOp.MUL, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));

            }
        }else if((lType.isFloat() && rType.isInteger()) || (lType.isInteger() && rType.isFloat())){
            quadOp = quadOp.convertToFloat();
            resultType = DataType.getFloat();
        }else if(lType != rType){
            throw new InvalidOperation(String.format("Can't do operation '%s' on pointer with type %s on line %d", op.literal, lResult.type.name, op.line));
        }

        Symbol out = Compiler.generateSymbol(resultType);
        l.addQuad(QuadOp.PUSH, out, null, out);
        l.concat(r);
        l.addQuad(QuadOp.MOV_REG_CA, rResult, null,rResult);
        l.addQuad(QuadOp.POP, lResult, null, lResult);
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
