package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.*;
/**
 * A logical expression is just either an boolean "and" or "or", which is defined with && and ||
 * @see Expression
 */
public class LogicalExpression extends Expression
{

    private final Expression left;
    private final Expression right;
    private final Token operation;
    public LogicalExpression(Expression left, Expression right, Token operation, int line, String file){
        super(line, file);
        this.left = left;
        this.right = right;
        this.operation = operation;
    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws  CompileException {

        // note that by just compiling both sides and creating a logical operation
        // we don't short circuit expressions since both values will be evaluated before
        left.compile(symbolTable, intermediates);
        Symbol leftResult = intermediates.getLastResult();
        right.compile(symbolTable, intermediates);
        Symbol rightResult = intermediates.getLastResult();

        intermediates.createLogical(symbolTable, leftResult, rightResult, this.operation.type());

    }
}
