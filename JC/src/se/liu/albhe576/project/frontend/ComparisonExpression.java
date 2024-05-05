package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.DataType;
import se.liu.albhe576.project.backend.SymbolTable;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.Intermediate;
import se.liu.albhe576.project.backend.IntermediateOperation;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;
/**
 * Expression for every type of comparison, i.e (<, ==) etc
 * Any valid comparison is between integer, floating point values and pointers
 * @see Expression
 */
public class ComparisonExpression extends Expression
{
    private final Expression left;
    private final Expression right;
    private final Token operation;
    public ComparisonExpression(Expression left, Expression right, Token operation, int line, String file){
        super(line,file);
        this.left = left;
        this.right = right;
        this.operation = operation;
    }

    private void typecheckComparison(DataType left, DataType right) {
        boolean leftIsValid = left.isNumber() || left.isPointer();
        boolean rightIsValid = right.isNumber() || right.isPointer();
        if(!(leftIsValid && rightIsValid)){
            Compiler.panic(String.format("Can't do comparison op %s with types %s and %s", this.operation.literal(), left, right), line, file);
        }
    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates)  throws CompileException{
        // Compile left and right side, type check the expression and convert to the correct types
        left.compile(symbolTable, intermediates);
        Symbol lResult = intermediates.getLastResult();

        IntermediateList rightIntermediates = new IntermediateList();
        right.compile(symbolTable, rightIntermediates);
        Symbol rResult = rightIntermediates.getLastResult();

        this.typecheckComparison(lResult.getType(), rResult.getType());
        DataType resultType = DataType.getHighestDataTypePrecedence(lResult.getType(), rResult.getType());

        IntermediateOperation op    = IntermediateOperation.fromToken(this.operation.type());

        lResult                     = Intermediate.convertType(symbolTable, intermediates, lResult, resultType);
        rResult                     = Intermediate.convertType(symbolTable, rightIntermediates, rResult, resultType);

        intermediates.addAll(rightIntermediates);
        intermediates.createComparison(symbolTable, op, lResult, rResult);
    }
}
