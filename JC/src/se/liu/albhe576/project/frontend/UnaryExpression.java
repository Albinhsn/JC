package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.SymbolTable;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.IntermediateOperation;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;

/**
 * A unary expression is an expression with a infix operation prior to another expression
 * The types of unary expressions within the language is
 *  - &foo, which takes a reference to foo
 *  - *foo, which dereferences foo
*  - ++foo, which does the postfix increment and stores the incremented value as the result of the operation (as opposed to ++ which stores the loaded value)
 * - !foo, is the negation of the expression, for this language this just means a comparison to 0
 * - "-foo", which is just the negation of an integer value
 * @see Expression
 */
public class UnaryExpression extends Expression
{
    private final Expression expression;
    private final Token operation;
    public UnaryExpression(Expression expression, Token operation, int line, String file){
        super(line, file);
        this.expression = expression;
        this.operation = operation;
    }
    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException {
        expression.compile(symbolTable, intermediates);
        Symbol source       = intermediates.getLastOperand1();
        Symbol lastResult   = intermediates.getLastResult();

         switch(this.operation.type()){
             // take a reference to something
            case TOKEN_AND_BIT -> {
                // similar to assignment we need to modify the load into just getting the pointer
                AssignStatement.takeReference(symbolTable, intermediates, source, this.line, this.file);
            }
             // dereference something
            case TOKEN_STAR -> {
                if(!lastResult.getType().isPointer()){
                   Compiler.panic("Can't dereference something that isn't a pointer or array?", line, file);
                }
                intermediates.createDereference(symbolTable, lastResult);
            }
             // negate something
            case TOKEN_MINUS -> {
                if(!lastResult.getType().isNumber()){
                    Compiler.panic("Can't negate something that isn't a integer or a float?", line, file);
                }
                intermediates.createNegate(symbolTable, lastResult);
            }
             // not !1
            case TOKEN_BANG -> {
                IntermediateOperation op = IntermediateOperation.fromToken(this.operation.type());
                intermediates.createLogicalNot(symbolTable, lastResult, op);
            }
             // inc or dec g
            case TOKEN_INCREMENT, TOKEN_DECREMENT -> {
                IntermediateOperation lastOp = intermediates.getLastIntermediate().op();
                if(!(lastOp == IntermediateOperation.DEREFERENCE || lastOp == IntermediateOperation.LOAD || lastOp == IntermediateOperation.INDEX || lastOp == IntermediateOperation.LOAD_MEMBER || lastOp == IntermediateOperation.LOAD_MEMBER_POINTER)){
                    Compiler.panic("Can't inc this expression?", line, file);
                }
                lastResult = AssignStatement.takeReference(symbolTable, intermediates, lastResult, this.line, this.file);
                intermediates.createPrefix(lastResult, this.operation.type());
            }
            default -> Compiler.panic(String.format("Invalid unary op? %s", this.operation.literal()), this.line, this.file);
         }
    }
}
