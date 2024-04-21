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

   private void callExternFunction(SymbolTable symbolTable, QuadList quads) throws CompileException {

       Function function = symbolTable.getFunction(this.name.literal());
       QuadOp[] generalRegisters = new QuadOp[]{
              QuadOp.MOV_RDI,
               QuadOp.MOV_RSI,
               QuadOp.MOV_RDX,
               QuadOp.MOV_RCX,
               QuadOp.MOV_R8,
               QuadOp.MOV_R9,
       };
       QuadOp[] floatRegisters = new QuadOp[]{
               QuadOp.MOV_XMM0,
               QuadOp.MOV_XMM1,
               QuadOp.MOV_XMM2,
               QuadOp.MOV_XMM3,
               QuadOp.MOV_XMM4,
               QuadOp.MOV_XMM5,
       };

       int generalCount = 0;
       int floatCount = 0;

       List<StructField> functionArgs = function.getArguments();
       if(functionArgs != null && args.size() > functionArgs.size()){
           this.error(String.format("Mismatch in argument count when calling %s", this.name.literal()));
       }

       Symbol floatType = Compiler.generateSymbol(DataType.getFloat());
       for (int i = 0; i < args.size(); i++) {
           Expr arg = args.get(i);
           QuadList argQuads = new QuadList();
           arg.compile(symbolTable, argQuads);
           Symbol result = argQuads.getLastResult();
           if (result.type.isFloatingPoint()) {
               if(floatCount >= 1){
                   quads.createPush(floatType);
               }
               quads.addAll(argQuads);
               quads.addQuad(floatRegisters[floatCount], null, null, null);
               if(floatCount >= 1){
                   quads.createPop(floatType);
               }

               floatCount++;
           } else {
               quads.addAll(argQuads);
               quads.addQuad(generalRegisters[generalCount], null, null, null);
               generalCount++;
           }

           if(functionArgs != null){
               DataType argType = functionArgs.get(i).type();
               if(!result.type.isSameType(argType)){
                   this.error(String.format("Type mismatch when calling extern function %s, expected %s got %s", this.name.literal(), argType.name, result.type.name));
               }
           }

           if(generalCount > generalRegisters.length || floatCount > floatRegisters.length){
               this.error(String.format("Can't call library function with more then %d ints and %d floats, you called %d, %d", generalCount, floatCount, generalRegisters.length, floatRegisters.length));
           }

       }

       quads.createCall(function.getFunctionSymbol(this.name.literal()), Compiler.generateSymbol(function.getReturnType()));
   }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{

       if(!symbolTable.functionExists(name.literal())){
           this.error(String.format("Trying to call undeclared function %s", name.literal()));

       }
        if(symbolTable.isExternFunction(name.literal())){
            this.callExternFunction(symbolTable, quads);
            return;
        }

        Function function = symbolTable.getFunction(this.name.literal());
        List<StructField> functionArguments = function.getArguments();
        if(args.size() != functionArguments.size()){
            this.error(String.format("Function parameter mismatch expected %d got %d on line %d when calling %s", functionArguments.size(), args.size(), name.line(), name.literal()));
        }

        QuadList argQuads = new QuadList();
        int argSize = 0;
        for(int i = 0; i < args.size(); i++){
            Expr arg = args.get(i);

            arg.compile(symbolTable, argQuads);
            Quad lastQuad = argQuads.getLastQuad();
            Symbol argSymbol = lastQuad.getResult();
            DataType funcArgType = functionArguments.get(i).type();
            if(!argSymbol.type.isSameType(funcArgType)){
                this.error(String.format("Function parameter type mismatch expected %s got %s", argSymbol.type.name, funcArgType.name));
            }
            argQuads.createMoveArgument(argSymbol, argSize);
            argSize += symbolTable.getStructSize(argSymbol.type);
        }
        if(argSize > 0){
            if(argSize % 16 == 8){
                argSize += 8;
            }
            quads.allocateArguments(argSize);
            quads.addAll(argQuads);
        }

        quads.createCall(function.getFunctionSymbol(this.name.literal()), Compiler.generateSymbol(function.getReturnType()));
    }
}
