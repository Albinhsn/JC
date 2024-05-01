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

   private int callExternalFunction(SymbolTable symbolTable, QuadList argumentQuads) throws CompileException {
       int floatCount = 0, generalCount = 0;
       final int maxCount       = 6;
       int argumentStackSize    = 0;
       final int rcx_location   = 4;

       QuadList floatQuads = new QuadList();
       QuadList generalQuads = new QuadList();
       for (Expr argument : this.args) {
           QuadList currentArgument = new QuadList();

           argument.compile(symbolTable, currentArgument);
           Symbol result = currentArgument.getLastResult();
           int argumentSize = symbolTable.getStructSize(result.type);

           if (result.type.isFloatingPoint()) {
               if (result.type.isFloat()) {
                   currentArgument.createConvert(symbolTable, result, DataType.getDouble());
               }
               currentArgument.createParam(currentArgument.getLastResult(), argumentStackSize, floatCount, true);
               if (floatCount >= maxCount) {
                   argumentStackSize += argumentSize;
               }
               floatCount++;

               currentArgument.addAll(floatQuads);
               floatQuads = currentArgument;

           } else {
               currentArgument.createParam(currentArgument.getLastResult(), argumentStackSize, generalCount, true);
               if (generalCount >= maxCount) {
                   argumentStackSize += argumentSize;
               }
               generalCount++;
               if (generalCount >= rcx_location) {
                   generalQuads.addAll(currentArgument);
               } else {
                   currentArgument.addAll(generalQuads);
                   generalQuads = currentArgument;
               }
           }
       }

       argumentQuads.addAll(floatQuads);
       argumentQuads.addAll(generalQuads);

       return argumentStackSize;

   }
   private int callInternalFunction(SymbolTable symbolTable, QuadList argumentQuads, Function function) throws CompileException {
       // Typecheck params
       int argumentStackSize = 0;
       List<StructField> params = function.getArguments();
       for(int i = 0 ; i < this.args.size(); i++){
           Expr argument = this.args.get(i);
           argument.compile(symbolTable, argumentQuads);
           DataType paramType = params.get(i).type();
           int argumentSize = symbolTable.getStructSize(paramType);

           Symbol result = argumentQuads.getLastResult();

           if(!paramType.isSameType(result.type) && !paramType.canBeConvertedTo(result.type)){
                Compiler.error(String.format("Parameter mismatch expected %s got %s", paramType, result.type), line, file);
           }else if(!paramType.isSameType(result.type)){
                result = argumentQuads.createConvert(symbolTable, result, paramType);
           }

           argumentQuads.createParam(result, argumentStackSize, 0, false);
           argumentStackSize += argumentSize;
       }
       return argumentStackSize;
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

       QuadList argumentQuads = new QuadList();
       int argumentStackSize;
        if(function.external){
            argumentStackSize = this.callExternalFunction(symbolTable, argumentQuads);
        }else{
            argumentStackSize = this.callInternalFunction(symbolTable, argumentQuads, function);
        }


       if(argumentStackSize != 0){
           argumentStackSize += Compiler.getStackAlignment(argumentStackSize);
           quads.createAllocate(argumentStackSize);
       }
       quads.addAll(argumentQuads);
       quads.createCall(symbolTable, function.getFunctionSymbol(functionName));
    }
}
