package se.liu.albhe576.project;

import java.util.List;

public class CallExpr extends Expr{
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(name.literal).append("(");
        for(int i = 0; i < args.size(); i++){
            s.append(args.get(i));
            if(i != args.size() - 1){
               s.append(", ");
            }
        }
        s.append(")");
        return s.toString();
    }

   private final Token name;
   private final List<Expr> args;
   public CallExpr(Token name, List<Expr> args, int line){
       super(line);
       this.name = name;
       this.args = args;
   }

   private QuadList handleSizeOf(SymbolTable symbolTable) throws CompileException, UnknownSymbolException {
       QuadList quads = new QuadList();
       if(args.size() != 1){
           throw new CompileException(String.format("Can't do sizeof with anything other then 1 arg on line %d", name.line));
       }
       Expr arg = args.get(0);
       if(!(arg instanceof VarExpr varExpr)){
           throw new CompileException(String.format("Can't do sizeof on something other then a var expression on line %d", name.line));
       }
       Struct struct = symbolTable.structs.get(varExpr.token.literal);
       int size = struct.getSize(symbolTable.structs);
       quads.addQuad(QuadOp.LOAD_IMM,  Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(size)),null, Compiler.generateSymbol(DataType.getInt()));
       return quads;
   }
   private QuadList callExternFunction(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {

       Function function = symbolTable.getExternFunction(this.name.literal);
       QuadOp[] argLocations = new QuadOp[]{
              QuadOp.MOV_RDI,
               QuadOp.MOV_RSI,
               QuadOp.MOV_RDX,
               QuadOp.MOV_REG_CA,
               QuadOp.MOV_R8,
               QuadOp.MOV_R9,
       };
       QuadOp[] floatLocations = new QuadOp[]{
               QuadOp.MOV_XMM0,
               QuadOp.MOV_XMM1,
               QuadOp.MOV_XMM2,
               QuadOp.MOV_XMM3,
               QuadOp.MOV_XMM4,
               QuadOp.MOV_XMM5,
       };
       if(this.args.size() > argLocations.length){
           throw new CompileException(String.format("Can't call library function with more then 6 arguments on line %d", this.line));
       }

       int generalCount = 0;
       int floatCount = 0;

       QuadList out = new QuadList();
       Symbol floatType = Compiler.generateSymbol(DataType.getFloat());
       for (Expr arg : args) {
           QuadList argQuad = arg.compile(symbolTable);
           Symbol result = argQuad.getLastResult();
           if (result.type.type == DataTypes.FLOAT) {
               if(floatCount > 1){
                   argQuad.addQuad(QuadOp.PUSH, floatType, null, floatType);
               }
               argQuad.addQuad(floatLocations[floatCount], null, null, null);
               if(floatCount > 1){
                   argQuad.addQuad(QuadOp.POP, floatType, null, floatType);
               }
               floatCount++;
           } else {
               argQuad.addQuad(argLocations[generalCount], null, null, null);
               generalCount++;
           }

           out.concat(argQuad);
       }

       out.addQuad(QuadOp.CALL, function.getFunctionSymbol(), null, Compiler.generateSymbol(function.returnType));
       return out;
   }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException {

        if(name.literal.equals("sizeof")){
            return this.handleSizeOf(symbolTable);
        }
        if(symbolTable.isExternFunction(name.literal) || symbolTable.isExternFunction(name.literal)){
            return this.callExternFunction(symbolTable);
        }

        QuadList quads = new QuadList();

        Function function = symbolTable.getFunction(this.name.literal);
        List<StructField> functionArguments = function.arguments;
        if(args.size() != functionArguments.size()){
            throw new CompileException(String.format("Function parameter mismatch expected %d got %d on line %d when calling %s", functionArguments.size(), args.size(), name.line, name.literal));
        }

        for(int i = args.size() - 1; i >= 0; i--){
            Expr arg = args.get(i);

            QuadList argQuad = arg.compile(symbolTable);
            Quad lastQuad = argQuad.getLastQuad();
            Symbol argSymbol = lastQuad.result;
            DataType funcArgType = functionArguments.get(i).type;
            if(!argSymbol.type.isSameType(funcArgType)){
                throw new CompileException(String.format("Function parameter type mismatch expected %s got %s on line %d", argSymbol.type.name, funcArgType.name, name.line));
            }
            quads.concat(argQuad);

            if(argSymbol.type.type == DataTypes.STRUCT){
                quads.addQuad(QuadOp.PUSH_STRUCT, argSymbol, null, null);
            }else{
                quads.addQuad(QuadOp.PUSH, argSymbol, null, null);
            }

        }

        quads.addQuad(QuadOp.CALL, function.getFunctionSymbol(), null, Compiler.generateSymbol(function.returnType));

        return quads;
    }
}
