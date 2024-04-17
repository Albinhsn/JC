package se.liu.albhe576.project;

import java.util.ArrayList;
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
       Struct struct = SymbolTable.lookupStruct(symbolTable.structs, varExpr.token.literal);
       int size = struct.getSize();
       quads.addQuad(QuadOp.LOAD_IMM,  Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(size)),null, Compiler.generateSymbol(DataType.getInt()));
       return quads;
   }
   private QuadList callLibrary(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {

       Function function = symbolTable.getLibraryFunction(this.name.literal);
       QuadOp[] argLocations = new QuadOp[]{
              QuadOp.MOV_RDI,
               QuadOp.MOV_RSI,
               QuadOp.MOV_RDX,
               QuadOp.MOV_REG_CA,
               QuadOp.MOV_R8,
               QuadOp.MOV_R9,
       };
       if(this.args.size() > argLocations.length){
           throw new CompileException(String.format("Can't call library function with more then 6 arguments on line %d", this.line));
       }

       QuadList out = new QuadList();
       for(int i = 0; i < args.size(); i++){
           Expr arg = args.get(i);
           QuadList argQuad = arg.compile(symbolTable);
           argQuad.addQuad(argLocations[i], null, null, null);
           out.concat(argQuad);
       }

       out.addQuad(QuadOp.CALL_LIBRARY, function.getFunctionSymbol(), null, Compiler.generateSymbol(function.returnType));
       return out;
   }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException {

        if(name.literal.equals("sizeof")){
            return this.handleSizeOf(symbolTable);
        }
        if(symbolTable.isLibraryFunction(name.literal)){
            return this.callLibrary(symbolTable);
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

            // This will have only loaded the pointer into rax, but we need to get every field onto the arg
            // Should be recursive once we support struct in struct
            if(argSymbol.type.type == DataTypes.STRUCT){
                Symbol varPointer = lastQuad.operand1;
                quads.addQuad(QuadOp.MOV_REG_CA,null, null, null);

                Struct struct = SymbolTable.lookupStruct(symbolTable.structs, argSymbol.type.name);

                for(int j = struct.fields.size() - 1; j >= 0; j--){
                    StructField field = struct.fields.get(j);
                    quads.addQuad(QuadOp.GET_FIELD, varPointer, new Symbol(field.name, field.type), Compiler.generateSymbol(field.type));
                    quads.addQuad(QuadOp.PUSH, null, null, null);
                    quads.addQuad(QuadOp.MOV_REG_AC, null, null, null);
                }
            }else{
                quads.addQuad(QuadOp.PUSH, argSymbol, null, null);
            }

        }

        quads.addQuad(QuadOp.CALL, function.getFunctionSymbol(), null, Compiler.generateSymbol(function.returnType));
        return quads;
    }
}
