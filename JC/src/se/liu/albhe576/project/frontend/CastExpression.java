package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.*;

/**
 * Expression for casting, i.e "int foo = (int)bar;" etc
 * When compiled will either generate a CONVERT intermediate instruction or just token CAST intermediate that changes the result symbol to be of the casted type
 * This is done since the only difference between say an integer pointer and a float pointer is what type it points to and no conversion is actually needed
 * @see Expression
 */
public class CastExpression extends Expression
{
    public CastExpression(DataType type, Expression expression, int line, String file) {
        super(line, file);
        this.expression = expression;
        this.type = type;
    }
    private final Expression expression;
    private final DataType type;
    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException{
        expression.compile(symbolTable, intermediates);
        Symbol result = intermediates.getLastResult();
        if(result.getType().isPointer() && type.isPointer()){
            // Cast is just a token intermediate whose result is of the correct type.
            // Casting a pointer to another is just changing the result
            // We create a token intermediate instead of changing the previous expression due to reliance on the operands
            intermediates.createCast(symbolTable, result, type);
        }else{
            intermediates.createConvert(symbolTable, result, type);
        }
    }
}
