package se.liu.albhe576.project;

import java.util.List;

public class CallExpr extends Expr{
   private final Token name;
   private final List<Expr> args;

   private static final QuadOp[] LINUX_GENERAL_ARGUMENT_LOCATIONS = {
           QuadOp.MOV_RDI,
           QuadOp.MOV_RSI,
           QuadOp.MOV_RDX,
           QuadOp.MOV_RCX,
           QuadOp.MOV_R8,
           QuadOp.MOV_R9,
   };
    private static final QuadOp[] LINUX_FLOATING_POINT_ARGUMENT_LOCATIONS = {
            QuadOp.MOV_XMM0,
            QuadOp.MOV_XMM1,
            QuadOp.MOV_XMM2,
            QuadOp.MOV_XMM3,
            QuadOp.MOV_XMM4,
            QuadOp.MOV_XMM5,
    };
   public CallExpr(Token name, List<Expr> args, int line, String file){
       super(line, file);
       this.name = name;
       this.args = args;
   }

   private void callExternFunction(SymbolTable symbolTable, QuadList quads) throws CompileException {

       Function function = symbolTable.getFunction(this.name.literal());

       int generalRegistersLength = LINUX_GENERAL_ARGUMENT_LOCATIONS.length;
       int floatRegistersLength = LINUX_FLOATING_POINT_ARGUMENT_LOCATIONS.length;
       int generalCount = 0;
       int floatCount = 0;

       List<StructField> functionArgs = function.getArguments();
       if(functionArgs != null && args.size() > functionArgs.size()){
           Compiler.error(String.format("Mismatch in argument count when calling %s", this.name.literal()), line, file);
       }

       Symbol floatType = Compiler.generateSymbol(DataType.getFloat());
       for (int i = 0; i < args.size(); i++) {
           Expr arg = args.get(i);
           QuadList argQuads = new QuadList();
           arg.compile(symbolTable, argQuads);
           Symbol result = argQuads.getLastResult();

           if(generalCount >= generalRegistersLength || floatCount >= floatRegistersLength){
               Compiler.error(String.format("Can't call library function with more then %d ints and %d floats, you called %d, %d", generalCount, floatCount, generalRegistersLength, floatRegistersLength), line, file);
           }

           if (result.type.isFloatingPoint()) {
               if(floatCount >= 1){
                   quads.createPush(floatType);
               }
               quads.addAll(argQuads);
               quads.addQuad(LINUX_FLOATING_POINT_ARGUMENT_LOCATIONS[floatCount], result, null, null);
               if(floatCount >= 1){
                   quads.createPop(floatType);
               }

               floatCount++;
           } else {
               quads.addAll(argQuads);
               quads.addQuad(LINUX_GENERAL_ARGUMENT_LOCATIONS[generalCount], result, null, null);
               generalCount++;
           }

           if(functionArgs != null){
               DataType argType = functionArgs.get(i).type();
               if(!result.type.isSameType(argType)){
                   Compiler.error(String.format("Type mismatch when calling extern function %s, expected %s got %s", this.name.literal(), argType.name, result.type.name), line, file);
               }
           }


       }

       quads.createCall(function.getFunctionSymbol(this.name.literal()), Compiler.generateSymbol(function.getReturnType()));
   }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{

       String functionName = name.literal();
       if(!symbolTable.functionExists(functionName)){
           Compiler.error(String.format("Trying to call undeclared function %s", functionName), line, file);

       }
        if(symbolTable.isExternFunction(functionName)){
            this.callExternFunction(symbolTable, quads);
            return;
        }

        Function function = symbolTable.getFunction(functionName);
        List<StructField> functionArguments = function.getArguments();
        if(args.size() != functionArguments.size()){
            Compiler.error(String.format("Function parameter mismatch expected %d got %d on line %d when calling %s", functionArguments.size(), args.size(), name.line(), name.literal()), line, file);
        }

        QuadList argQuads = new QuadList();
        int argSize = 0;
        for(int i = 0; i < args.size(); i++){
            Expr arg = args.get(i);

            arg.compile(symbolTable, argQuads);
            Quad lastQuad = argQuads.getLastQuad();
            Symbol argSymbol = lastQuad.result();

            DataType funcArgType = functionArguments.get(i).type();
            argSymbol = AssignStmt.convertValue(argSymbol, Compiler.generateSymbol(funcArgType), argQuads);

            if(!argSymbol.type.isSameType(funcArgType)){
                Compiler.error(String.format("Function parameter type mismatch expected %s got %s", funcArgType.name,argSymbol.type.name), line, file);
            }

            argQuads.createMoveArgument(argSymbol, argSize);
            argSize += SymbolTable.getStructSize(symbolTable.getStructs(), argSymbol.type);
        }

        if(argSize > 0){
            if(argSize % 16 != 0){
                argSize += 16 - (argSize % 16);
            }
            quads.allocateArguments(argSize);
            quads.addAll(argQuads);
        }

        quads.createCall(function.getFunctionSymbol(functionName), Compiler.generateSymbol(function.getReturnType()));
    }
}
