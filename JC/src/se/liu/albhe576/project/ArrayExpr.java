package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class ArrayExpr extends Expr{

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[");
        for(int i = 0; i < items.size(); i++){
            s.append(items.get(i));
            if(i < items.size() - 1){
                s.append(", ");
            }
        }
        s.append("]");
        return s.toString();
    }

    private final List<Expr> items;

    public ArrayExpr(List<Expr> items, int line){
        super(line);
        this.items = items;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {

        // We declare the amount of stack space we need up front for the array
        // Then we push the items up
        // And lastly we store some pointer to the beginning
        QuadList quads = new QuadList();
        List<Symbol> argSymbols = new ArrayList<>();
        for(Expr item : this.items){
            quads.concat(item.compile(symbolTable));
            quads.addQuad(QuadOp.PUSH, null, null, quads.getLastResult());
            argSymbols.add(quads.getLastResult());
        }
        Symbol first = argSymbols.get(0);
        for(int i = 1; i < argSymbols.size(); i++){
            if(!first.type.isSameType(argSymbols.get(i).type)){
                throw new CompileException(String.format("Can't have different types in array declaration on line %d", this.line));
            }
        }
        quads.addQuad(QuadOp.LEA_RSP, null, null, Compiler.generateSymbol(DataType.getPointerFromType(first.type)));

        return quads;
    }
}
