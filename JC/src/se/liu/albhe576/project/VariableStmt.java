package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class VariableStmt implements Stmt{

    @Override
    public String toString() {
        return String.format("%s %s = %s;", type.name, name, value);
    }

    private final DataType type;
    private final String name;
    private final Expr value;
    public VariableStmt(DataType type, String name, Expr value){
        this.name = name;
        this.type = type;
        this.value = value;
    }

    private void checkValidTypes(Symbol lastResult, Symbol lastOperand, List<Quad> quads) throws CompileException {
        if(lastResult.type.type == DataTypes.FLOAT && type.type.isInteger()){
            quads.add(new Quad(QuadOp.CVTTSS2SI, lastResult, null, Compiler.generateSymbol(DataType.getInt())));
            return;
        }
        if(lastResult.type.type.isInteger() && type.type == DataTypes.FLOAT){
            quads.add(new Quad(QuadOp.CVTDQ2PD, lastResult, null, Compiler.generateSymbol(DataType.getFloat())));
            return;
        }

        if(!lastResult.type.isSameType(type) && !lastOperand.isNull()){
            throw new CompileException(String.format("Trying to access type %s to type %s", lastResult.type.name, type.name));
        }
    }

    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {

        if(symbolTable.symbolExists(name)){
            throw new CompileException(String.format("Trying to redeclare existing variable %s\n", name));
        }
        List<Quad> val = value.compile(symbolTable);

        // ToDo check valid conversion instead of just the same
        Symbol lastOperand = Quad.getLastOperand1(val);
        Symbol lastSymbol = Quad.getLastResult(val);

        this.checkValidTypes(lastSymbol, lastOperand, val);

        Symbol variable = new Symbol(name, type);

        val.add(new Quad(QuadOp.STORE, lastSymbol, null, variable));
        symbolTable.addSymbol(variable);
        return val;
    }
}
