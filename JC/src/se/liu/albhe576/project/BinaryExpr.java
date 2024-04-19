package se.liu.albhe576.project;

public class BinaryExpr extends Expr{

    public Expr left;
    public Expr right;
    public Token op;

    @Override
    public String toString() {
        return String.format("(%s %s %s)", left, op.literal, right);
    }

    public BinaryExpr(Expr left, Token op, Expr right, int line, String file){
        super(line, file);
        this.left = left;
        this.op = op;
        this.right = right;
    }


    private void bitwise(SymbolTable symbolTable, QuadList quads) throws InvalidOperation, CompileException, UnexpectedTokenException, UnknownSymbolException {

        left.compile(symbolTable, quads);
        Symbol lResult = quads.getLastResult();
        Symbol rResult = quads.createSetupBinary(symbolTable, right, lResult);

        if(lResult.type.type != DataTypes.INT || rResult.type.type != DataTypes.INT){
            throw new InvalidOperation(String.format("Can only do bitwise on ints, line %d", op.line));
        }
        quads.addQuad(QuadOp.fromToken(op), lResult, rResult, Compiler.generateSymbol(DataType.getInt()));
    }
    private void arithmetic(SymbolTable symbolTable, QuadList quads) throws InvalidOperation, CompileException, UnexpectedTokenException, UnknownSymbolException {

        left.compile(symbolTable, quads);
        Symbol lResult = quads.getLastResult();
        DataType lType = lResult.type;

        QuadList r = new QuadList();
        right.compile(symbolTable, r);
        Symbol rResult = r.getLastResult();
        DataType rType = rResult.type;

        if(lResult.type.isStruct() || rResult.type.isStruct()){
            throw new InvalidOperation(String.format("Can't do operation '%s' on struct on line %d", op.literal, op.line));
        }

        DataType resultType = lResult.type;
        QuadOp quadOp = QuadOp.fromToken(op);

        if(lType.isFloatingPoint() || rType.isFloatingPoint()){
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
                quads.addQuad(QuadOp.MOV_REG_CA, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));
                quads.addQuad(QuadOp.LOAD_IMM,Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(structSize)), null, Compiler.generateSymbol(DataType.getInt()));
                quads.addQuad(QuadOp.MUL, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));

            }
        }else if((lType.isFloatingPoint() && rType.isInteger()) || (lType.isInteger() && rType.isFloatingPoint())){
            quadOp = quadOp.convertToFloat();
            resultType = DataType.getFloat();
        }else if(!lType.isSameType(rType)){
            System.out.println(lType);
            System.out.println(rType);
            throw new InvalidOperation(String.format("Can't do operation '%s' on pointer with type %s on line %d", op.literal, lResult.type.name, op.line));
        }

        Symbol out = Compiler.generateSymbol(resultType);
        quads.createPush(out);
        quads.addAll(r);
        quads.addQuad(QuadOp.MOV_REG_CA, rResult, null,rResult);
        quads.createPop(lResult);
        quads.addQuad(quadOp, lResult, rResult, Compiler.generateSymbol(resultType));
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException {
        switch(op.type){
            // These only work on everything except string
            case TOKEN_PLUS : {}
            case TOKEN_MINUS : {}
            case TOKEN_SLASH: {}
            case TOKEN_MOD: {}
            case TOKEN_STAR: {
                this.arithmetic(symbolTable, quads);
                return;
            }

            // These only work on pointers, int and byte
            case TOKEN_AND_BIT:{}
            case TOKEN_OR_BIT: {}
            case TOKEN_XOR : {}
            case TOKEN_SHIFT_LEFT: {}
            case TOKEN_SHIFT_RIGHT: {
                this.bitwise(symbolTable, quads);
                return;
            }
        }
        throw new InvalidOperation(String.format("Can't do binary op with '%s'", op.literal));
    }
}
