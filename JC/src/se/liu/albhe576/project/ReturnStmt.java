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
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        QuadList out = new QuadList();
        Function currentFunction = symbolTable.getCurrentFunction();
        if(!(expr instanceof EmptyExpr)){
            out.concat(expr.compile(symbolTable));
            Symbol returnSymbol = out.getLastResult();
            if(returnSymbol.type.type.isInteger() && currentFunction.returnType.type == DataTypes.FLOAT){
                out.addQuad(QuadOp.CVTSI2SS, null, null, null);
            }else if(currentFunction.returnType.type.isInteger() && returnSymbol.type.type == DataTypes.FLOAT){
                out.addQuad(QuadOp.CVTTSS2SI, null, null, null);
            }
            else if(!returnSymbol.type.isSameType(currentFunction.returnType)){
                throw new CompileException(String.format("Mismatch in return type in function %s, expected %s got %s", currentFunction.name, currentFunction.returnType.name, returnSymbol.type.name));
            }

        }
        else if(currentFunction.returnType.type != DataTypes.VOID){
            throw new CompileException(String.format("Expected return value in function %s", currentFunction.name));
        }
        out.addQuad(QuadOp.RET, null, null, null);
        return out;
    }
}
