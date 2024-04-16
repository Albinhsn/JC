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
    public List<Quad> compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {
        List<Quad> val = value.compile(symbolTable);
        List<Quad> targetVariable = variable.compile(symbolTable);


        if(variable instanceof DotExpr dotExpr){
            targetVariable.remove(targetVariable.size() - 1);
            Symbol result = Quad.getLastOperand1(targetVariable);
            targetVariable.add(new Quad(QuadOp.PUSH, null, null, null));
            targetVariable.addAll(val);
            targetVariable.add(new Quad(QuadOp.MOV_REG_CA, null, null, null));
            targetVariable.add(new Quad(QuadOp.POP, null, null, null));
            targetVariable.add(new Quad(QuadOp.SET_FIELD, null, symbolTable.getMemberSymbol(result, dotExpr.member.literal), result));
            return targetVariable;

        }

        // Figure out if legal?
        Symbol res = Quad.getLastResult(val);

        val.addAll(targetVariable);
        val.add(new Quad(QuadOp.STORE, res, null, Quad.getLastResult(targetVariable)));

        return val;
    }
}
