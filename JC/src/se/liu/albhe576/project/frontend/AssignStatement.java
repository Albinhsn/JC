package se.liu.albhe576.project.frontend;


import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;
import se.liu.albhe576.project.backend.Intermediate;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.IntermediateOperation;
import se.liu.albhe576.project.backend.MemberSymbol;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.SymbolTable;

/**
 * Class for all assignments (not declarations)
 * This includes assigning to index, dereference and member fields as well
 * @see Statement
 */
public class AssignStatement extends Statement
{
    private final Expression variable;
    private final Expression value;

    public AssignStatement(Expression variable, Expression value, int line, String file){
        super(line, file);
        this.variable = variable;
        this.value = value;
    }

    public static boolean isInvalidAssignment(Symbol variableType, Symbol valueResult, Symbol lastOperand){
        return !(variableType.getType().canBeConvertedTo(valueResult.getType())  || lastOperand.isNull());
    }

    public static Symbol takeReference(SymbolTable symbolTable, IntermediateList variableIntermediates, Symbol variableSymbol, int line, String file) {
        // when compiling the left side of an assignment (i.e this = ...)
        // It will naviely load the variable which means the last result will be the type and not the pointer to which we want to store
        // therefore we still allow the expression to be compiled but remove the last load of the value and load the pointer to it instead

        Intermediate loaded = variableIntermediates.pop();
        IntermediateOperation lastOp = loaded.op();
        if(lastOp == IntermediateOperation.LOAD){
            variableIntermediates.createLoadPointer(symbolTable, loaded.operand1());
        }else if(lastOp == IntermediateOperation.LOAD_MEMBER){
            variableIntermediates.createLoadMemberPointer(symbolTable, (MemberSymbol) loaded.operand1(), loaded.result().getType());
            variableSymbol = loaded.result();
        }else if(lastOp == IntermediateOperation.INDEX){
            variableIntermediates.createReferenceIndex(symbolTable, loaded.operand1(), loaded.operand2());
            variableSymbol = symbolTable.generateSymbol(loaded.operand1().getType().getTypeFromPointer());
        } else if(lastOp != IntermediateOperation.DEREFERENCE){
                Compiler.panic("Can't take unary from this expression?", line, file);
        }
        return variableSymbol;
    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException {
        // Compile the value
        value.compile(symbolTable, intermediates);
        Symbol valueResult      = intermediates.getLastResult();
        Symbol valueOperand = intermediates.getLastOperand1();

        // Compile the variable
        IntermediateList variableIntermediates  = new IntermediateList();
        variable.compile(symbolTable, variableIntermediates);
        Symbol variable         = variableIntermediates.getLastOperand1();
        Symbol variableType     = variableIntermediates.getLastResult();

        // Typecheck and convert if needed
        if(isInvalidAssignment(variableType, valueResult, valueOperand)){
            Compiler.panic(String.format("Trying to assign type %s to %s", valueResult.getType(), variableType.getType()), line, file);
        } else if(!valueResult.getType().isSameType(variableType.getType())){
            valueResult = intermediates.createConvert(symbolTable, valueResult, variableType.getType());
        }

        // create the assignment
        variable = takeReference(symbolTable, variableIntermediates, variable, this.line, this.file);
        intermediates.addAll(variableIntermediates);
        intermediates.createAssign(valueResult, variable);
    }
}
