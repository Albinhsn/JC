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

       Function function = symbolTable.getExternFunction(this.name.literal);
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

       Symbol floatType = Compiler.generateSymbol(DataType.getFloat());
       for (Expr arg : args) {
           arg.compile(symbolTable, quads);
           Symbol result = quads.getLastResult();
           if (result.type.isFloatingPoint()) {

               if(floatCount > 1){quads.createPush(floatType);}
               quads.addQuad(floatRegisters[floatCount], null, null, null);
               if(floatCount > 1){quads.createPop(floatType);}

               floatCount++;
           } else {
               quads.addQuad(generalRegisters[generalCount], null, null, null);
               generalCount++;
           }

           if(generalCount >= generalRegisters.length || floatCount >= floatRegisters.length){
               this.error(String.format("Can't call library function with more then %d ints and %d floats, you called %d, %d", generalCount, floatCount, generalRegisters.length, floatRegisters.length));
           }

       }

       quads.createCall(function.getFunctionSymbol(), Compiler.generateSymbol(function.returnType));
   }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException{

        if(symbolTable.isExternFunction(name.literal) || symbolTable.isExternFunction(name.literal)){
            this.callExternFunction(symbolTable, quads);
            return;
        }

        Function function = symbolTable.getFunction(this.name.literal);
        List<StructField> functionArguments = function.arguments;
        if(args.size() != functionArguments.size()){
            this.error(String.format("Function parameter mismatch expected %d got %d on line %d when calling %s", functionArguments.size(), args.size(), name.line, name.literal));
        }

        for(int i = args.size() - 1; i >= 0; i--){
            Expr arg = args.get(i);

            arg.compile(symbolTable, quads);
            Quad lastQuad = quads.getLastQuad();
            Symbol argSymbol = lastQuad.result;
            DataType funcArgType = functionArguments.get(i).type;
            if(!argSymbol.type.isSameType(funcArgType)){
                this.error(String.format("Function parameter type mismatch expected %s got %s", argSymbol.type.name, funcArgType.name));
            }
            if(argSymbol.type.isStruct()){
                quads.createPushStruct(argSymbol);
            }else{
                quads.createPush(argSymbol);
            }

        }

        quads.createCall(function.getFunctionSymbol(), Compiler.generateSymbol(function.returnType));
    }
}
