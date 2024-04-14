package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class AssignExpr implements  Expr{
    @Override
    public String toString() {
        return String.format("%s = %s",variable, value);
    }
    private final Expr variable;
    private final Expr value;

    public AssignExpr(Expr variable, Expr value){
        this.variable = variable;
        this.value = value;
    }

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws CompileException, UnknownSymbolException {
        List<Quad> val = value.compile(symbolTable);
        List<Quad> targetVariable = variable.compile(symbolTable);

        // Figure out if legal?
        Symbol res = Quad.getLastResult(val);
        val.addAll(targetVariable);
        val.add(new Quad(QuadOp.STORE, res, null, Quad.getLastResult(targetVariable)));

        return val;
    }
}
