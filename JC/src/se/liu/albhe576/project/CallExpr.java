package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class CallExpr implements Expr{
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
   public CallExpr(Token name, List<Expr> args){
       this.name = name;
       this.args = args;
   }

   private List<Quad> handleSizeOf(SymbolTable symbolTable) throws CompileException, UnknownSymbolException {
       List<Quad> quads = new ArrayList<>();
       if(args.size() != 1){
           throw new CompileException(String.format("Can't do sizeof with anything other then 1 arg on line %d", name.line));
       }
       Expr arg = args.get(0);
       if(!(arg instanceof VarExpr)){
           throw new CompileException(String.format("Can't do sizeof on something other then a var expression on line %d", name.line));
       }
       VarExpr varExpr = (VarExpr) arg;
       Struct struct = SymbolTable.lookupStruct(symbolTable.structs, varExpr.token.literal);
       int size = struct.getSize();
       quads.add(new Quad(QuadOp.LOAD_IMM,  Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(size)),null, Compiler.generateSymbol(DataType.getInt())));
       return quads;
   }
   private List<Quad> callLibrary(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {

       Function function = symbolTable.getFunction(this.name.literal);
       List<StructField> functionArguments = function.arguments;
       if(args.size() != 1){
           throw new CompileException(String.format("Function parameter mismatch expected %d got %d on line %d when calling %s", functionArguments.size(), args.size(), name.line, name.literal));
       }

       Expr arg = this.args.get(0);

       List<Quad> quads = new ArrayList<>(arg.compile(symbolTable));
       quads.add(new Quad(QuadOp.MOV_RDI, null, null, null));
       quads.add(new Quad(QuadOp.CALL, function.getFunctionSymbol(), null, Compiler.generateSymbol(function.returnType)));
       return quads;
   }

    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException {

        if(name.literal.equals("sizeof")){
            return this.handleSizeOf(symbolTable);
        }
        if(symbolTable.isLibraryFunction(name.literal)){
            return this.callLibrary(symbolTable);
        }

        List<Quad> quads = new ArrayList<>();

        Function function = symbolTable.getFunction(this.name.literal);
        List<StructField> functionArguments = function.arguments;
        if(args.size() != functionArguments.size()){
            throw new CompileException(String.format("Function parameter mismatch expected %d got %d on line %d when calling %s", functionArguments.size(), args.size(), name.line, name.literal));
        }

        for(int i = args.size() - 1; i >= 0; i--){
            Expr arg = args.get(i);

            List<Quad> argQuad = arg.compile(symbolTable);
            Quad lastQuad = argQuad.get(argQuad.size() - 1);
            Symbol argSymbol = lastQuad.result;
            DataType funcArgType = functionArguments.get(i).type;
            if(!argSymbol.type.isSameType(funcArgType)){
                throw new CompileException(String.format("Function parameter type mismatch expected %s got %s on line %d", argSymbol.type.name, funcArgType.name, name.line));
            }
            quads.addAll(argQuad);

            // This will have only loaded the pointer into rax, but we need to get every field onto the arg
            // Should be recursive once we support struct in struct
            if(argSymbol.type.type == DataTypes.STRUCT){
                Symbol varPointer = lastQuad.operand1;
                quads.add(new Quad(QuadOp.MOV_REG_CA,null, null, null));

                Struct struct = SymbolTable.lookupStruct(symbolTable.structs, argSymbol.type.name);

                for(int j = struct.fields.size() - 1; j >= 0; j--){
                    StructField field = struct.fields.get(j);
                    quads.add(new Quad(QuadOp.GET_FIELD, varPointer, new Symbol(field.name, field.type), Compiler.generateSymbol(field.type)));
                    quads.add(new Quad(QuadOp.PUSH, null, null, null));
                    quads.add(new Quad(QuadOp.MOV_REG_AC, null, null, null));
                }
            }else{
                quads.add(new Quad(QuadOp.PUSH, argSymbol, null, null));
            }

        }

        quads.add(new Quad(QuadOp.CALL, function.getFunctionSymbol(), null, Compiler.generateSymbol(function.returnType)));
        return quads;
    }
}
