package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.Compiler;
import se.liu.albhe576.project.backend.DataType;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.VariableSymbol;
import se.liu.albhe576.project.backend.SymbolTable;
/**
 * Referes to the declaration of a variable (not the assignment of one)
 * i.e int a = 5, not a = 5
 * @see AssignStatement
 * @see Statement
 */
public class VariableStatement extends Statement
{
    private final DataType type;
    private final String name;
    private final Expression value;
    public VariableStatement(DataType type, String name, Expression value, int line, String file){
        super(line, file);
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException {
        if(symbolTable.symbolExists(name)){
            Compiler.panic(String.format("Trying to redeclare existing variable %s\n", name), line, file);
        }

        if(!symbolTable.isDeclaredStruct(type.getName())){
            Compiler.panic(String.format("Trying to declare variable with non existing type? %s\n", type.getName()), line, file);
        }

        VariableSymbol variable = symbolTable.addVariable(name, type);
        if(value != null){
            value.compile(symbolTable, intermediates);
            Symbol lastOperand      = intermediates.getLastOperand1();
            Symbol lastSymbol       = intermediates.getLastResult();

            if(AssignStatement.isInvalidAssignment(variable, lastSymbol, lastOperand)){
                Compiler.panic(String.format("Trying to assign type %s to type %s", lastSymbol.getType(), type), line, file);
            }
            if(!type.isSameType(lastSymbol.getType())){
                lastSymbol = intermediates.createConvert(symbolTable, lastSymbol, type);
            }

            Symbol loadedVariable = intermediates.createLoadPointer(symbolTable, variable);
            intermediates.createAssign(lastSymbol, loadedVariable);
        }
    }
}
