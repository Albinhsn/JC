package se.liu.albhe576.project.frontend;

import java.util.List;
import se.liu.albhe576.project.backend.DataType;
import se.liu.albhe576.project.backend.Function;
import se.liu.albhe576.project.backend.SymbolTable;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;

/**
 * Expression for any function call (including calls to external functions)
 * When a call is done it will compile the parameters but leave them on the stack and resolve their actual location when the call gets transformed in code generation
 * The reason for doing this is to avoid hardcoding any logic regarding calling conventions and be able to resolve that when we do code generation
 * @see Expression
 * @see Function
 * @see CallingConvention
 */
public class CallExpression extends Expression
{
   private final Token name;
   private final List<Expression> args;
   public CallExpression(Token name, List<Expression> args, int line, String file){
       super(line, file);
       this.name = name;
       this.args = args;
   }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException{

       // Does the function already exist?
       String functionName = name.literal();
       if(!symbolTable.functionExists(functionName)){
           Compiler.panic(String.format("Trying to call undeclared function %s", functionName), line, file);
       }

       Function function                    = symbolTable.getFunction(functionName);
       List<StructureField> functionArguments  = function.getArguments();

        // correct number of arguments? (or varargs)
       if(!function.isVarArgs() && args.size() > functionArguments.size()){
            Compiler.panic(String.format("Function parameter mismatch expected %d got %d when calling %s", functionArguments.size(), args.size(), name.literal()), line, file);
       }

       // Compile every argument and store the value on the stack
       // We store the amount of stackSpace the arguments take for internal structs and the count of both general and floating point arguments
       // In order to make it easier to comply to a ceratin calling convention and essentially defer where the arguments are supposed to go when we generate the assembly
       int generalCount = 0, floatingPointCount = 0;
       int stackSpace = 0;
       for(int argumentIndex = 0; argumentIndex < args.size(); argumentIndex++){
            Expression argument = args.get(argumentIndex);
            argument.compile(symbolTable, intermediates);
            Symbol result       = intermediates.getLastResult();
            DataType resultType =result.getType();

           // typecheck if needed
            if(!function.isVarArgs()){
                DataType paramType = functionArguments.get(argumentIndex).type();
                stackSpace += symbolTable.getStructureSize(paramType);

                if(!paramType.isSameType(resultType) && !paramType.canBeConvertedTo(resultType)){
                    Compiler.panic(String.format("Parameter mismatch expected %s got %s", paramType, resultType), line, file);
                }else if(!paramType.isSameType(resultType)){
                    intermediates.createConvert(symbolTable, result, paramType);
                }
            }

            if(resultType.isFloatingPoint()){
                floatingPointCount++;
            }else{
                generalCount++;
            }
        }

       intermediates.createCall(symbolTable, function.getFunctionSymbol(functionName, generalCount, floatingPointCount, stackSpace));
    }
}
