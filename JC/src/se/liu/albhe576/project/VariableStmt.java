package se.liu.albhe576.project;

public class VariableStmt extends Stmt{

    @Override
    public String toString() {
        return String.format("%s %s = %s;", type.name, name, value);
    }

    private final DataType type;
    private final String name;
    private final Expr value;
    public VariableStmt(DataType type, String name, Expr value, int line, String file){
        super(line, file);
        this.name = name;
        this.type = type;
        this.value = value;
    }

    private void checkValidTypes(Symbol lastResult, Symbol lastOperand, QuadList quads) throws CompileException {
        if(lastResult.type.isFloatingPoint() && type.isInteger()){
            quads.addQuad(QuadOp.CONVERT_FLOAT_TO_INT, lastResult, null, Compiler.generateSymbol(DataType.getInt()));
            return;
        }
        if(lastResult.type.isInteger() && type.isFloatingPoint()){
            quads.addQuad(QuadOp.CONVERT_INT_TO_FLOAT, lastResult, null, Compiler.generateSymbol(DataType.getFloat()));
            return;
        }

        if(!lastResult.type.isSameType(type) && !lastOperand.isNull()){
            this.error(String.format("Trying to access type %s to type %s", lastResult.type, type));
        }
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {

        if(symbolTable.symbolExists(name)){
            this.error(String.format("Trying to redeclare existing variable %s\n", name));
        }

        if(!symbolTable.isDeclaredStruct(type.name)){
            this.error(String.format("Trying to declare variable with non existing type? %s\n", type.name));
        }

        VariableSymbol variable = symbolTable.addVariable(name, type);
        if(value != null){
            value.compile(symbolTable, quads);
            Symbol lastOperand = quads.getLastOperand1();
            Symbol lastSymbol = quads.getLastResult();

            this.checkValidTypes(lastSymbol, lastOperand, quads);

            if(type.isStruct()){
                quads.createPush(lastSymbol);
                Symbol loadedPointer = quads.createLoadPointer(variable);
                quads.createMovRegisterAToC(loadedPointer);
                quads.createPop(lastSymbol);
            }

            quads.createStore(variable);
        }



    }
}
