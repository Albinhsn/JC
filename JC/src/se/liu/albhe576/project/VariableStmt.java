package se.liu.albhe576.project;

import java.util.List;

public class VariableStmt extends Stmt{

    @Override
    public String toString() {
        return String.format("%s %s = %s;", type.name, name, value);
    }

    private final DataType type;
    private final String name;
    private final Expr value;
    public VariableStmt(DataType type, String name, Expr value, int line){
        super(line);
        this.name = name;
        this.type = type;
        this.value = value;
    }

    private void checkValidTypes(Symbol lastResult, Symbol lastOperand, QuadList quads) throws CompileException {
        if(lastResult.type.type == DataTypes.FLOAT && type.type.isInteger()){
            quads.addQuad(QuadOp.CVTTSS2SI, lastResult, null, Compiler.generateSymbol(DataType.getInt()));
            return;
        }
        if(lastResult.type.type.isInteger() && type.type == DataTypes.FLOAT){
            quads.addQuad(QuadOp.CVTSI2SS, lastResult, null, Compiler.generateSymbol(DataType.getFloat()));
            return;
        }

        if(!lastResult.type.isSameType(type) && !lastOperand.isNull()){
            throw new CompileException(String.format("Trying to access type %s to type %s", lastResult.type.name, type.name));
        }
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {

        QuadList valueQuads = new QuadList();
        Symbol lastSymbol = null;
        if(symbolTable.symbolExists(name)){
            throw new CompileException(String.format("Trying to redeclare existing variable %s\n", name));
        }else if(value != null){
            valueQuads.concat(value.compile(symbolTable));
            Symbol lastOperand = valueQuads.getLastOperand1();
            lastSymbol = valueQuads.getLastResult();

            this.checkValidTypes(lastSymbol, lastOperand, valueQuads);
        }

        Symbol variable = new Symbol(name, type);
        valueQuads.addQuad(QuadOp.STORE, lastSymbol, null, variable);
        symbolTable.addSymbol(variable);
        return valueQuads;
    }
}
