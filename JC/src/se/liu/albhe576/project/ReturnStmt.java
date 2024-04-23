package se.liu.albhe576.project;


import java.util.ArrayList;
import java.util.List;

public class ReturnStmt extends Stmt{
    @Override
    public String toString() {
        return String.format("return %s;", expr);
    }
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
            Symbol returnSymbol = QuadList.convertResultToCorrectType(quads, quads.getLastResult(), Compiler.generateSymbol(returnType));


            if(!returnSymbol.type.isSameType(returnType) && !returnSymbol.type.canBeCastedTo(returnType)){
                this.error(String.format("Mismatch in return type in function %s, expected %s got %s", currentFunction, returnType.name, returnSymbol.type.name));
            }


        }
        else if(returnType.type != DataTypes.VOID){
            this.error(String.format("Expected return value in function %s", functionName));
        }
        quads.createReturn();
    }
}
