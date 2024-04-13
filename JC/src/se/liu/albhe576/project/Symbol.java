package se.liu.albhe576.project;

import java.util.List;

public class Symbol {
    private final SymbolType type;
    private final List<StructField> fields;

    public Value value;
    private final String name;
    public static int getSize(List<List<Symbol>> symbols){
        return symbols.get(symbols.size() - 1).size();
    }

    public static Value lookupSymbol(List<List<Symbol>> symbols, Token literal){
        return null;
    }

    public SymbolType getType(){
        return type;
    }

    public Symbol(SymbolType type, List<StructField> fields, String name, Value value){
        this.type = type;
        this.fields = fields;
        this.name = name;
        this.value = value;
    }
}
