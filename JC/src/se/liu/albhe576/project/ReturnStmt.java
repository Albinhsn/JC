package se.liu.albhe576.project;


public class ReturnStmt extends Stmt{
    private final Expr expr;
    public ReturnStmt(Expr expr, int line, String file){
        super(line, file);
        this.expr = expr;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        DataType returnType = symbolTable.getCurrentFunctionReturnType();

        if(expr != null){
            expr.compile(symbolTable, quads);
            Symbol returnSymbol = quads.getLastResult();

            if(!returnSymbol.type.canBeConvertedTo(returnType) && !returnSymbol.type.isSameType(returnType)){
                Compiler.error(String.format("Mismatch in return type in function %s, expected %s got %s", symbolTable.getCurrentFunctionName(), returnType, returnSymbol.type), line, file);
            }
            if(!returnSymbol.type.isSameType(returnType)){
                returnSymbol = quads.createConvert(symbolTable, returnSymbol, returnType);
            }
            quads.createReturn(returnSymbol);
        }
        else if(returnType.type != DataTypes.VOID){
            Compiler.error(String.format("Expected return value in function %s", symbolTable.getCurrentFunctionName()), line, file);
        }else{
            quads.createReturn(null);
        }
    }
}
