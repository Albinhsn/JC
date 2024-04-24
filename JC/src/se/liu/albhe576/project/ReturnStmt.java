package se.liu.albhe576.project;


public class ReturnStmt extends Stmt{
    private final Expr expr;
    public ReturnStmt(Expr expr, int line, String file){
        super(line, file);
        this.expr = expr;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        Function currentFunction = symbolTable.getCurrentFunction();
        String functionName = symbolTable.getCurrentFunctionName();
        DataType returnType = currentFunction.getReturnType();

        if(expr != null){
            expr.compile(symbolTable, quads);
            Symbol returnSymbol = AssignStmt.convertValue(quads.getLastResult(), Compiler.generateSymbol(returnType), quads);

            if(!returnSymbol.type.isSameType(returnType) && !returnSymbol.type.canBeCastedTo(returnType)){
                Compiler.error(String.format("Mismatch in return type in function %s, expected %s got %s", currentFunction, returnType.name, returnSymbol.type.name), line, file);
            }
        }
        else if(returnType.type != DataTypes.VOID){
            Compiler.error(String.format("Expected return value in function %s", functionName), line, file);
        }
        quads.createReturn();
    }
}
