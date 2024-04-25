package se.liu.albhe576.project;

public class CastExpr extends Expr{
    public CastExpr(DataType type, Expr expr, int line, String file) {
        super(line, file);
        this.expr = expr;
        this.type = type;
    }
    private final Expr expr;
    private final DataType type;
    @Override
    void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
        expr.compile(symbolTable, quads);
        Symbol result = quads.getLastResult();
        if(result.type.isPointer() && type.isPointer()){
            Quad lastQuad = quads.pop();
            quads.addQuad(lastQuad.op(), lastQuad.operand1(), lastQuad.operand2(), Compiler.generateSymbol(type));
        }else{
            AssignStmt.convertValue(result, Compiler.generateSymbol(type), quads);
        }
    }
}