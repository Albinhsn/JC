package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.DataType;
import se.liu.albhe576.project.backend.SymbolTable;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;
/**
 * An index expression is an just an index of a pointer, string or array.
 * The usage in the language is "foo[]"
 * @see Expression
 */
public class IndexExpression extends Expression
{
    private final Expression value;
    private final Expression index;
    private static boolean isInvalidValueToIndex(DataType type){return !(type.isString() || type.isArray() || type.isPointer());}
    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws  CompileException{

        // Compile the value
        value.compile(symbolTable, intermediates);
        Symbol valResult = intermediates.getLastResult();

        // Compile the index
        index.compile(symbolTable, intermediates);
        Symbol indexResult = intermediates.getLastResult();

        // Typecheck both the value and the index
        if(isInvalidValueToIndex(valResult.getType())){
            Compiler.panic(String.format("Can't index type %s", valResult.getType()), line, file);
        }
        else if(!indexResult.getType().isNumber()){
            Compiler.panic(String.format("Can only use integer as index not %s", indexResult.getType()), line, file);
        }
        // if the index is a float we emit a convert to make it an integer (or long in this case)
        else if(indexResult.getType().isFloatingPoint()){
            indexResult = intermediates.createConvert(symbolTable, indexResult, DataType.getLong());
        }

        intermediates.createIndex(symbolTable, valResult, indexResult);
    }
    public IndexExpression(Expression value, Expression index, int line, String file){
        super(line, file);
        this.value = value;
        this.index = index;

    }
}
