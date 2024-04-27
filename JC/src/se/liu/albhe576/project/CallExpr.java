package se.liu.albhe576.project;

import java.util.List;

public class CallExpr extends Expr{
   private final Token name;
   private final List<Expr> args;
   private static final QuadOp[] LINUX_GENERAL_ARGUMENT_LOCATIONS = {QuadOp.MOV_RDI, QuadOp.MOV_RSI, QuadOp.MOV_RDX, QuadOp.MOV_RCX, QuadOp.MOV_R8, QuadOp.MOV_R9};
   private static final QuadOp[] LINUX_FLOATING_POINT_ARGUMENT_LOCATIONS = {QuadOp.MOV_XMM0, QuadOp.MOV_XMM1, QuadOp.MOV_XMM2, QuadOp.MOV_XMM3, QuadOp.MOV_XMM4, QuadOp.MOV_XMM5};
   public CallExpr(Token name, List<Expr> args, int line, String file){
       super(line, file);
       this.name = name;
       this.args = args;
   }
   private QuadList[] compileAndTypeCheckArgument(SymbolTable symbolTable, List<StructField> functionArgs) throws CompileException {
       boolean varArgs          = functionArgs == null;
       int numberOfArguments    = this.args.size();
       QuadList[] quads         = new QuadList[numberOfArguments];

       for(int i = 0; i < numberOfArguments; i++){
            Expr arg = this.args.get(i);
            QuadList argQuads = new QuadList();
            arg.compile(symbolTable, argQuads);
            Symbol result = argQuads.getLastResult();

           if(!varArgs){
               DataType argType = functionArgs.get(i).type();
               result = AssignStmt.convertValue(result, Compiler.generateSymbol(argType), argQuads);
               if(!result.type.isSameType(argType)){
                   Compiler.error(String.format("Type mismatch when calling extern function %s, expected %s got %s", this.name.literal(), argType.name, result.type.name), line, file);
               }
           }
           quads[i] = argQuads;
       }
       return quads;
   }

   private void addArgument(QuadList quads, QuadList argQuads, Symbol result, QuadOp[] argumentLocations, int count, int pushCount){
       if(count >= pushCount){
           quads.createPush(Compiler.generateSymbol(result.type));
       }
       quads.addAll(argQuads);
       quads.addQuad(argumentLocations[count], result, null, null);
       if(count >= pushCount){
           quads.createPop(Compiler.generateSymbol(result.type));
       }

   }
   private void compileExternFunctionArguments(SymbolTable symbolTable, QuadList quads, List<StructField> functionArgs) throws CompileException {

       int generalRegistersLength = LINUX_GENERAL_ARGUMENT_LOCATIONS.length;
       int floatRegistersLength   = LINUX_FLOATING_POINT_ARGUMENT_LOCATIONS.length;
       int generalCount = 0, floatCount = 0;
       final int pushGeneralCount = 4, pushFloatCount = 1;

       QuadList[] args = this.compileAndTypeCheckArgument(symbolTable, functionArgs);

       for (QuadList argQuads : args) {
           Symbol result = argQuads.getLastResult();

           if(generalCount >= generalRegistersLength || floatCount >= floatRegistersLength){
               Compiler.error(String.format("Can't call library function with more then %d ints and %d floats, you called %d, %d", generalCount, floatCount, generalRegistersLength, floatRegistersLength), line, file);
           }

           if (result.type.isFloat() || result.type.isDouble()) {
               this.addArgument(quads, argQuads, result, LINUX_FLOATING_POINT_ARGUMENT_LOCATIONS, floatCount, pushFloatCount);
               floatCount++;
           } else {
               this.addArgument(quads, argQuads, result, LINUX_GENERAL_ARGUMENT_LOCATIONS, generalCount, pushGeneralCount);
               generalCount++;
           }
       }
   }
   private void compileInternalFunctionArguments(SymbolTable symbolTable, QuadList quads, List<StructField> functionArguments) throws CompileException {
       QuadList[] argQuads = this.compileAndTypeCheckArgument(symbolTable, functionArguments);

       int argSize = 0;
       for(int i = 0; i < argQuads.length; i++){
           QuadList argQuad = argQuads[i];
           Symbol argSymbol = argQuad.getLastResult();

           argSymbol = AssignStmt.convertValue(argSymbol, Compiler.generateSymbol(functionArguments.get(i).type()), argQuad);

           if(argSymbol.type.isStruct()){
               argQuad.pop();
           }
           argQuad.createMoveArgument(argSymbol, argSize);
           argSize += SymbolTable.getStructSize(symbolTable.getStructs(), argSymbol.type);
       }

       if(argSize > 0){
           argSize += Compiler.getStackPadding(argSize);
           quads.allocateArguments(argSize);
           for(QuadList argQuad : argQuads){
               quads.addAll(argQuad);
           }
       }
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
            Compiler.error(String.format("Function parameter mismatch expected %d got %d on line %d when calling %s", functionArguments.size(), args.size(), name.line(), name.literal()), line, file);
       }

       if(symbolTable.isExternFunction(functionName)){
            this.compileExternFunctionArguments(symbolTable, quads, functionArguments);
       }else{
            this.compileInternalFunctionArguments(symbolTable, quads, functionArguments);
       }
       quads.addQuad(QuadOp.CALL, function.getFunctionSymbol(functionName), null, Compiler.generateSymbol(function.getReturnType()));
    }
}
