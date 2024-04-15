package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public abstract class Symbol {

    @Override
    public String toString() {
        return name;
    }

    public String name;

    public String getName(){
        return this.name;
    }

    public static Symbol findSymbol(Stack<List<Symbol>> symbolTable, String name) throws UnknownSymbolException {

        for(List<Symbol> table : symbolTable){
            for(Symbol symbol : table){
                if(symbol.getName().equals(name)){
                    return symbol;
                }
            }
        }
        throw new UnknownSymbolException(String.format("Can't find symbol %s in the table",name));
    }

    public static int calculateStackOffset(List<Symbol> symbols, String variable){
        return 0;
    }
    public Symbol(String name){
        this.name = name;

    }
}
