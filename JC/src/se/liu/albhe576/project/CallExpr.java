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

       Function function = symbolTable.getFunction(functionName);
       List<StructField> functionArguments = function.getArguments();

       if(functionArguments != null && args.size() > functionArguments.size()){
            Compiler.error(String.format("Function parameter mismatch expected %d got %d when calling %s", functionArguments.size(), args.size(), name.literal()), line, file);
       }

       for(Expr argument : args){
           argument.compile(symbolTable, quads);
           // Typecheck params
           quads.createParam(quads.getLastResult());
       }

       quads.createCall(function.getFunctionSymbol(functionName));
    }
}
