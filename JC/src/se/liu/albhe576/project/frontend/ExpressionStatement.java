package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.*;
/**
 * Just a Statement with an expression, the main type of expressions that will be here are either redundant ones such as "1 + 2" without any assignment and
 * function calls
 * @see Statement
 * @see Expression
 */
public class ExpressionStatement extends Statement
{
    private final Expression expression;
    public ExpressionStatement(Expression expression, int line, String file){
        super(line, file);
        this.expression = expression;
    }
    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws  CompileException{
        if(expression != null){
            expression.compile(symbolTable, intermediates);
        }
    }
}
