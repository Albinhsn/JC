package se.liu.albhe576.project;

import java.util.List;

public class CallExpr extends Expr{
   private final Token name;
   private final List<Expr> args;
   public CallExpr(Token name, List<Expr> args, int line, String file){
       super(line, file);
       this.name = name;
       this.args = args;
   }


    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{
       String functionName = name.literal();
       if(!symbolTable.functionExists(functionName)){
           Compiler.error(String.format("Trying to call undeclared function %s", functionName), line, file);
       }

       Function function                    = symbolTable.getFunction(functionName);
       List<StructField> functionArguments  = function.getArguments();

       if(functionArguments != null && args.size() > functionArguments.size()){
            Compiler.error(String.format("Function parameter mismatch expected %d got %d when calling %s", functionArguments.size(), args.size(), name.literal()), line, file);
       }

       // Since we need to allocate space on the stack prior to pushing arguments
       // We send a token quad list
       QuadList argumentQuads = new QuadList();
       int argumentStackSize = function.call(symbolTable, argumentQuads, args);

       if(argumentStackSize != 0){
           argumentStackSize += Compiler.getStackAlignment(argumentStackSize);
           quads.createAllocate(argumentStackSize);
       }
       quads.addAll(argumentQuads);
       quads.createCall(symbolTable, function.getFunctionSymbol(functionName), argumentStackSize);
    }
}
