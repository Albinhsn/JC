package se.liu.albhe576.project;


import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ReturnStmt implements Stmt{
    @Override
    public String toString() {
        return String.format("return %s;", expr);
    }
    private final Expr expr;
    public ReturnStmt(Expr expr){
        this.expr = expr;

    }


    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        List<Quad> out = new ArrayList<>();
        Function currentFunction = symbolTable.getCurrentFunction();
        if(!(expr instanceof EmptyExpr)){
            out.addAll(expr.compile(symbolTable));
            Symbol returnSymbol = Quad.getLastResult(out);
            if(returnSymbol.type.type.isInteger() && currentFunction.returnType.type == DataTypes.FLOAT){
                out.add(new Quad(QuadOp.CVTTSS2SI, null, null, null));
            }else if(currentFunction.returnType.type.isInteger() && returnSymbol.type.type == DataTypes.FLOAT){
                out.add(new Quad(QuadOp.CVTDQ2PD, null, null, null));
            }
            else if(!returnSymbol.type.isSameType(currentFunction.returnType)){
                throw new CompileException(String.format("Mismatch in return type in function %s, expected %s got %s", currentFunction.name, currentFunction.returnType.name, returnSymbol.type.name));
            }

        }
        else if(currentFunction.returnType.type != DataTypes.VOID){
            throw new CompileException(String.format("Expected return value in function %s", currentFunction.name));
        }
        out.add(new Quad(QuadOp.RET, null, null, null));
        return out;
    }
}
