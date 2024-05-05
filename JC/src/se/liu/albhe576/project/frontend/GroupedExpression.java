package se.liu.albhe576.project.frontend;
import se.liu.albhe576.project.backend.*;

/**
 * A grouped expression is just an expression within a parenthesis
 * @see Expression
 */
public class GroupedExpression extends Expression
{
    private final Expression expression;
    public GroupedExpression(Expression expression, int line, String file){
        super(line, file);
        this.expression = expression;
    }
    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException {
        expression.compile(symbolTable, intermediates);
    }
}
