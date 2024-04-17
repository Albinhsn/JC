package se.liu.albhe576.project;


public class UnaryExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s%s", op.literal, expr);
    }

    public Expr expr;
    public Token op;
    public UnaryExpr(Expr expr, Token op, int line){
        super(line);
        this.expr = expr;
        this.op = op;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        QuadList quads = expr.compile(symbolTable);
        Symbol symbol = quads.getLastResult();
        Symbol result = Compiler.generateSymbol(symbol.type);
        QuadOp quadOp;

        switch(op.type){
            // Take the address of a pointer
            // int foo = &bar;
            case TOKEN_AND_BIT:{
                // Remove load
                symbol = quads.getLastOperand1();
                quads.removeLastQuad();
                result = Compiler.generateSymbol(DataType.getPointerFromType(symbol.type));
                quadOp = QuadOp.LOAD_POINTER;
                break;
            }
            // Dereference
            case TOKEN_STAR:{
                quadOp = QuadOp.DEREFERENCE;
                symbol = quads.getLastOperand1();
                break;
            }
            case TOKEN_MINUS:{
               quadOp = QuadOp.NOT;
               break;
            }
            case TOKEN_INCREMENT:{
                if(result.type.type.isPointer()){
                    int structSize = symbolTable.getStructSize(result.type.name);
                    quads.addQuad(QuadOp.PUSH, result, null, Compiler.generateSymbol(result.type));
                    Symbol immSymbol = Compiler.generateSymbol(DataType.getInt());
                    quads.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(structSize)), null, immSymbol);
                    Symbol movedImm = Compiler.generateSymbol(DataType.getInt());
                    quads.addQuad(QuadOp.MOV_REG_CA, immSymbol, null, movedImm);
                    quads.addQuad(QuadOp.POP, null, null, Compiler.generateSymbol(result.type));
                    quads.addQuad(QuadOp.ADD, Compiler.generateSymbol(result.type), movedImm, Compiler.generateSymbol(result.type));
                    return quads;
                }
                quadOp = QuadOp.fromToken(op);
                break;
            }
            case TOKEN_DECREMENT:{
                if(result.type.type.isPointer()){
                    int structSize = symbolTable.getStructSize(result.type.name);
                    quads.addQuad(QuadOp.PUSH, result, null, Compiler.generateSymbol(result.type));
                    Symbol immSymbol = Compiler.generateSymbol(DataType.getInt());
                    quads.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(structSize)), null, immSymbol);
                    Symbol movedImm = Compiler.generateSymbol(DataType.getInt());
                    quads.addQuad(QuadOp.MOV_REG_CA, immSymbol, null, movedImm);
                    quads.addQuad(QuadOp.POP, null, null, Compiler.generateSymbol(result.type));
                    quads.addQuad(QuadOp.SUB, Compiler.generateSymbol(result.type), movedImm, Compiler.generateSymbol(result.type));
                    return quads;
                }
                quadOp = QuadOp.fromToken(op);
                break;
            }
            default:{
                quadOp = QuadOp.fromToken(op);
            }
        }

        quads.addQuad(quadOp, symbol, null, result);
        return quads;
    }
}
