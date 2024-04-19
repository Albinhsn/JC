package se.liu.albhe576.project;


import java.util.ArrayList;
import java.util.List;

public class ReturnStmt extends Stmt{
    @Override
    public String toString() {
        return String.format("return %s;", expr);
    }
    private final Expr expr;
    public ReturnStmt(Expr expr, int line){
        super(line);
        this.expr = expr;

    }


    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        Function currentFunction = symbolTable.getCurrentFunction();

        if(!(expr instanceof EmptyExpr)){
            expr.compile(symbolTable, quads);
            Symbol returnSymbol = quads.getLastResult();
            if(returnSymbol.type.isInteger() && currentFunction.returnType.isFloatingPoint()){
                quads.addQuad(QuadOp.CVTSI2SD, null, null, null);
            }else if(currentFunction.returnType.isInteger() && returnSymbol.type.isFloatingPoint()){
                quads.addQuad(QuadOp.CVTTSD2SI, null, null, null);
            }
            else if(!returnSymbol.type.isSameType(currentFunction.returnType)){
                throw new CompileException(String.format("Mismatch in return type in function %s, expected %s got %s", currentFunction.name, currentFunction.returnType.name, returnSymbol.type.name));
            }

        }
        else if(currentFunction.returnType.type != DataTypes.VOID){
            throw new CompileException(String.format("Expected return value in function %s", currentFunction.name));
        }
        quads.addQuad(QuadOp.RET, null, null, null);
    }
}
