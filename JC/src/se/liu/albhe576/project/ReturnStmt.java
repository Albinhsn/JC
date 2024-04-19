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

        if(expr != null){
            expr.compile(symbolTable, quads);
            Symbol returnSymbol = quads.getLastResult();
            if(returnSymbol.type.isInteger() && currentFunction.returnType.isFloatingPoint()){
                quads.createConvertIntToFloat(returnSymbol);
            }else if(currentFunction.returnType.isInteger() && returnSymbol.type.isFloatingPoint()){
                quads.createConvertFloatToInt(returnSymbol);
            }
            else if(!returnSymbol.type.isSameType(currentFunction.returnType)){
                this.error(String.format("Mismatch in return type in function %s, expected %s got %s", currentFunction.name, currentFunction.returnType.name, returnSymbol.type.name));
            }

        }
        else if(currentFunction.returnType.type != DataTypes.VOID){
            this.error(String.format("Expected return value in function %s", currentFunction.name));
        }
        quads.createReturn();
    }
}
