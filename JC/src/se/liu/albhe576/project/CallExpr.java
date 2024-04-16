package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class CallExpr implements Expr{
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(name.literal + "(");
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

    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException {
        List<Quad> quads = new ArrayList<>();

        Function function = symbolTable.getFunction(this.name.literal);
        List<StructField> functionArguments = function.arguments;
        if(args.size() != functionArguments.size()){
            throw new CompileException(String.format("Function parameter mismatch expected %d got %d on line %d", functionArguments.size(), args.size(), name.line));
        }

        for(int i = 0; i < args.size(); i++){
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

                Struct struct = symbolTable.lookupStruct(symbolTable.structs, argSymbol.type.name);

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

        // check valid call?
        quads.add(new Quad(QuadOp.CALL, function.getFunctionSymbol(), null, Compiler.generateSymbol(function.returnType)));
        return quads;
    }
}
