package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    public final List<Struct> structs;
    public final List<Function> functions;
    public final Map<String, String> constants;
    public final java.util.Stack<List<Symbol>> localSymbolTable;

    public void enterScope(){
        this.localSymbolTable.push(new ArrayList<>());
    }
    public void exitScope(){
        this.localSymbolTable.pop();
    }


    public void addSymbol(Symbol symbol){
        localSymbolTable.peek().add(symbol);
    }
    public Function getFunction(String name) throws UnknownSymbolException{
        for(Function function : functions){
            if(function.name.equals(name)){
                return function;
            }
        }
        throw new UnknownSymbolException(String.format("Can't find function %s", name));
    }

    public void addConstant(String constant){
        String constName = "const" + constants.size();
        constants.put(constant, constName);
    }

    public Symbol findSymbol(String name) throws UnknownSymbolException {
        for(List<Symbol> symbols : this.localSymbolTable){
            for(Symbol symbol : symbols){
                if(symbol.name.equals(name)){
                    return symbol;
                }
            }
        }
        throw new UnknownSymbolException(String.format("Can't find symbol of name %s", name));
    }
    public Symbol getMemberSymbol(Symbol structSymbol, String member) throws UnknownSymbolException {
        Struct struct = lookupStruct(this.structs, structSymbol.name);
        for(StructField field : struct.fields){
            if(field.name.equals(member)){
               return new Symbol(member, field.type);
            }
        }
        throw new UnknownSymbolException(String.format("Tried to access member '%s' in struct '%s', doesnt exist", member, structSymbol.name));
    }

    public static Struct lookupStruct(List<Struct> structs, String name) throws UnknownSymbolException {
        System.out.printf("Looking for %s\n", name);
        for(Struct struct : structs){
            System.out.printf("Checking vs %s\n", struct.type.name);
            if(struct.type.name.equals(name)){
                return struct;
            }
        }
        throw new UnknownSymbolException(String.format("Couldn't find struct '%s'", name));
    }


    public SymbolTable(){
        this.constants = new HashMap<>();
        this.structs = new ArrayList<>();
        this.functions = new ArrayList<>();
        this.localSymbolTable = new java.util.Stack<>();
    }
}
