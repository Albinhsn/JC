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
            Symbol operand1 = Quad.getLastOperand1(targetVariable);
            Symbol result = Quad.getLastResult(targetVariable);
            Symbol memberSymbol = symbolTable.getMemberSymbol(result, dotExpr.member.literal);


            targetVariable.add(new Quad(QuadOp.PUSH, result, null, null));
            targetVariable.addAll(val);
            Symbol valResult = Quad.getLastResult(val);
            if(memberSymbol.type.type != DataTypes.FLOAT){
                targetVariable.add(new Quad(QuadOp.MOV_REG_CA, valResult, null, Compiler.generateSymbol(valResult.type)));
            }
            targetVariable.add(new Quad(QuadOp.POP, null, null, Compiler.generateSymbol(result.type)));
            targetVariable.add(new Quad(QuadOp.SET_FIELD, null, memberSymbol, operand1));

            return targetVariable;

        }

        // Figure out if legal?
        Symbol res = Quad.getLastResult(val);

        val.addAll(targetVariable);
        val.add(new Quad(QuadOp.STORE, res, null, Quad.getLastResult(targetVariable)));

        return val;
    }
}
