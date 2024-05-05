package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.DataType;
import se.liu.albhe576.project.backend.SymbolTable;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.IntermediateOperation;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.Intermediate;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;

/**
 * Class for all binary expressions (bitwise and binary expressions, not logical and comparison expressions)
 * Valid arithmetic expressions are on any integer and floating point type, with two exceptions being modulo '%' between two integers and pointer arithmetic which only is done with integer values
 * @see Expression
 */
public class BinaryExpression extends Expression
{
    private final Expression left;
    private final Expression right;
    private final Token operation;
    public BinaryExpression(Expression left, Token operation, Expression right, int line, String file){
        super(line, file);
        this.left = left;
        this.operation = operation;
        this.right = right;
    }

    private boolean isInvalidBitwise(DataType left, DataType right){
        return !(left.isInteger() && right.isInteger());
    }
    private boolean isInvalidArithmetic(IntermediateOperation op, DataType left, DataType right)
    {
        if(op == IntermediateOperation.MOD && !(left.isInteger() && right.isInteger())){
            return true;
        }
        if((left.isPointer() && !right.isInt()) || (right.isPointer() && !left.isInt())){
            return true;
        }
        return left.isArray() || left.isStructure() || right.isArray() || right.isStructure();
    }
    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException{
        // Compile left and right side of the expression
        left.compile(symbolTable, intermediates);
        Symbol leftResult = intermediates.getLastResult();
        DataType leftType = leftResult.getType();

        IntermediateList rightIntermediates = new IntermediateList();
        right.compile(symbolTable, rightIntermediates);
        Symbol rightResult                  = rightIntermediates.getLastResult();
        DataType rightType = rightResult.getType();

        DataType resultType         = DataType.getHighestDataTypePrecedence(leftType, rightType);
        IntermediateOperation op    = IntermediateOperation.fromToken(this.operation.type());

        // Typecheck the results versus the operation that's being done
        if(op.isBitwise() && isInvalidBitwise(leftType, rightType)){
            Compiler.panic(String.format("Can't do bitwise op with %s and %s", leftType, rightType), line, file);
        }else if(isInvalidArithmetic(op, leftType, rightType)){
            Compiler.panic(String.format("Can't do arithmetic op with %s and %s", leftType, rightType), line, file);
        }

        // Convert values to it's correct type if needed
        leftResult  = Intermediate.convertType(symbolTable, intermediates, leftResult, resultType);
        leftType = leftResult.getType();
        rightResult = Intermediate.convertType(symbolTable, rightIntermediates, rightResult, resultType);
        rightType = rightResult.getType();

        // Doing pointer arithmetic i.e foo*; foo++ is transformed into foo + sizeof(foo)
        // so we do an immediate multiply with the other value (invalid to be anything other then a number) with the size of the underlying value
        if(leftType.isPointer()){
            rightIntermediates.createImmediateMultiply(symbolTable, rightResult, symbolTable.getStructureSize(leftType.getTypeFromPointer()));
        }else if(rightType.isPointer()) {
            intermediates.createImmediateMultiply(symbolTable, leftResult, symbolTable.getStructureSize(rightType.getTypeFromPointer()));
        }

        // add the right side and create the binary op
        intermediates.addAll(rightIntermediates);
        intermediates.createBinaryOp(symbolTable, op, leftResult, rightResult, resultType);
    }
}
