package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.DataType;
import se.liu.albhe576.project.backend.SymbolTable;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;
/**
 * A postfix expression is a "++" or "--" after the target expression
 * The infix version is located in UnaryExpression
 * @see Expression
 * @see UnaryExpression
 */
public class PostfixExpression extends Expression
{
    private final Expression target;
    private final Token operation;

    public PostfixExpression(Expression target, Token operation, int line, String file){
        super(line, file);
        this.target = target;
        this.operation = operation;
    }
    private static boolean isInvalidPostfixTarget(DataType type){return type.isArray() || type.isString() || type.isStructure();}
    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException {
        this.target.compile(symbolTable, intermediates);
        Symbol target = intermediates.getLastResult();
        DataType targetType = target.getType();
        if(isInvalidPostfixTarget(targetType)){
            Compiler.panic(String.format("Can't postfix type %s", targetType), line, file);
        }
        AssignStatement.takeReference(symbolTable, intermediates, target, line, file);
        intermediates.createPostfix(target, operation.type());
    }
}
