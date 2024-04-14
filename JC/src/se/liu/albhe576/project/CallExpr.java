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
            Symbol argSymbol = Quad.getLastResult(argQuad);
            quads.addAll(argQuad);
            quads.add(new Quad(QuadOp.PUSH, argSymbol, null, null));
        }

        // check valid call?
        Symbol function = Symbol.findSymbol(symbolTable, name.literal);
        quads.add(new Quad(QuadOp.CALL, function, null, Compiler.generateResultSymbol()));
        return quads;
    }
}
