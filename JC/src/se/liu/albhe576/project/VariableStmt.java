package se.liu.albhe576.project;

public class VariableStmt extends Stmt{
    private final DataType type;
    private final String name;
    private final Expr value;
    public VariableStmt(DataType type, String name, Expr value, int line, String file){
        super(line, file);
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        if(symbolTable.symbolExists(name)){
            Compiler.error(String.format("Trying to redeclare existing variable %s\n", name), line, file);
        }

        if(!symbolTable.isDeclaredStruct(type.name)){
            Compiler.error(String.format("Trying to declare variable with non existing type? %s\n", type.name), line, file);
        }

        VariableSymbol variable = symbolTable.addVariable(name, type);
        if(value != null){
            value.compile(symbolTable, quads);
            Symbol lastOperand = quads.getLastOperand1();
            Symbol lastSymbol = quads.getLastResult();

            Symbol variableType = Compiler.generateSymbol(type);
            lastSymbol = AssignStmt.convertValue(lastSymbol, variableType, quads);
            if(AssignStmt.isInvalidAssignment(variableType, lastSymbol, lastOperand)){
                Compiler.error(String.format("Trying to assign type %s to type %s", lastSymbol.type, type), line, file);
            }

            QuadList rQuads = new QuadList();
            Symbol loadedPointer = Compiler.generateSymbol(DataType.getPointerFromType(variable.type));
            rQuads.addQuad(QuadOp.LOAD_VARIABLE_POINTER, variable, null, loadedPointer);
            quads.createSetupBinary(rQuads, lastSymbol, loadedPointer);
            quads.createStoreVariable(variable);
        }
    }
}
