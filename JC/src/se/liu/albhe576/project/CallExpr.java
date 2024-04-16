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
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws CompileException, UnknownSymbolException {
        List<Quad> quads = new ArrayList<>();

        for(Expr arg : args){
            List<Quad> argQuad = arg.compile(symbolTable);
            Quad lastQuad = argQuad.get(argQuad.size() - 1);
            Symbol argSymbol = lastQuad.result;
            quads.addAll(argQuad);
            if(lastQuad.op == QuadOp.LOAD_POINTER){
                // Essentially create a new symbol of that size in the next scope
                VariableSymbol variableSymbol = (VariableSymbol) Symbol.findSymbol(symbolTable, lastQuad.operand1.name);
                Symbol varPointer = lastQuad.operand1;
                quads.add(new Quad(QuadOp.MOV_REG_DA,null, null, null));
                List<StructField> fields = variableSymbol.type.fields;
                for(int i = fields.size() - 1; i >= 0; i--){
                    StructField field = fields.get(i);
                    quads.add(new Quad(QuadOp.GET_FIELD, varPointer, new ResultSymbol(field.name), Compiler.generateResultSymbol()));
                    quads.add(new Quad(QuadOp.PUSH, null, null, null));
                    quads.add(new Quad(QuadOp.MOV_REG_AD, null, null, null));
                }
            }else{
                quads.add(new Quad(QuadOp.PUSH, argSymbol, null, null));
            }

        }

        // check valid call?
        Symbol function = Symbol.findSymbol(symbolTable, name.literal);
        quads.add(new Quad(QuadOp.CALL, function, null, Compiler.generateResultSymbol()));
        return quads;
    }
}
