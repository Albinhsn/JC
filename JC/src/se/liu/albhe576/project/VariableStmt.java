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
            quads.addQuad(QuadOp.CVTTSD2SI, lastResult, null, Compiler.generateSymbol(DataType.getInt()));
            return;
        }
        if(lastResult.type.type.isInteger() && type.type == DataTypes.FLOAT){
            quads.addQuad(QuadOp.CVTSI2SD, lastResult, null, Compiler.generateSymbol(DataType.getFloat()));
            return;
        }

        if(!lastResult.type.isSameType(type) && !lastOperand.isNull()){
            throw new CompileException(String.format("Trying to access type %s to type %s", lastResult.type.name, type.name));
        }
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {

        Symbol lastSymbol = null;
        if(symbolTable.symbolExists(name)){
            throw new CompileException(String.format("Trying to redeclare existing variable %s\n", name));
        }else if(value != null){
            value.compile(symbolTable, quads);
            Symbol lastOperand = quads.getLastOperand1();
            lastSymbol = quads.getLastResult();

            this.checkValidTypes(lastSymbol, lastOperand, quads);
        }

        if(!symbolTable.isDeclaredStruct(type.name)){
            throw new CompileException(String.format("Trying to declare variable with non existing type? %s\n", type.name));
        }

        VariableSymbol variable = symbolTable.addSymbol(name, type);
        quads.addQuad(QuadOp.STORE, lastSymbol, null, variable);
    }
}
