package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;
import se.liu.albhe576.project.backend.DataType;
import se.liu.albhe576.project.backend.DataTypes;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.SymbolTable;

/**
 * The statement for function return values in the language
 * @see Function
 * @see Expression
 */
public class ReturnStatement extends Statement
{
    private final Expression expression;
    public ReturnStatement(Expression expression, int line, String file){
        super(line, file);
        this.expression = expression;
    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException {
        DataType returnType = symbolTable.getCurrentFunctionReturnType();

        // check if return anything
        if(expression != null){

            // compile the return value, check if it's the correct type, convert if needed and emit a return instruction
            expression.compile(symbolTable, intermediates);
            Symbol returnSymbol = intermediates.getLastResult();
            DataType returnSymbolType = returnSymbol.getType();

            if(!returnSymbolType.canBeConvertedTo(returnType) && !returnSymbolType.isSameType(returnType)){
                Compiler.panic(String.format("Mismatch in return type in function %s, expected %s got %s", symbolTable.getCurrentFunctionName(), returnType, returnSymbolType), line, file);
            }
            else if(!returnSymbolType.isSameType(returnType)){
                returnSymbol = intermediates.createConvert(symbolTable, returnSymbol, returnType);
            }
            intermediates.createReturn(returnSymbol);
        }
        // panic if we don't return and we should
        else if(returnType.getType() != DataTypes.VOID){
            Compiler.panic(String.format("Expected return value in function %s", symbolTable.getCurrentFunctionName()), line, file);
        }else{
            intermediates.createReturn(null);
        }
    }
}
