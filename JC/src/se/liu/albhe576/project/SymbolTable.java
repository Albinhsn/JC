package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    public final List<Struct> structs;
    private final List<Function> functions;
    private final List<Function> libraryFunctions;
    private final Map<String, Constant> constants;
    private final java.util.Stack<List<Symbol>> localSymbolTable;

    public void enterScope(){
        this.localSymbolTable.push(new ArrayList<>());
    }
    public void enterScope(List<Symbol> symbols){
        this.localSymbolTable.push(symbols);
    }
    public void exitScope(){
        this.localSymbolTable.pop();
    }


    public Function getCurrentFunction(){
        return this.functions.get(this.functions.size() - 1);
    }
    public List<Function> getFunctions(){
        return this.functions;
    }
    public Map<String, Constant> getConstants(){
        return this.constants;
    }
    public void addSymbol(Symbol symbol){

        localSymbolTable.peek().add(symbol);
    }
    public void addFunction(Function function){
        this.functions.add(function);
    }
    public Function getFunction(String name) throws UnknownSymbolException{
        for(Function function : functions){
            if(function.name.equals(name)){
                return function;
            }
        }
        for(Function function : this.libraryFunctions){
            if(function.name.equals(name)){
                return function;
            }
        }
        throw new UnknownSymbolException(String.format("Can't find function %s", name));
    }

    public void addConstant(String constant, DataTypes type){
        String constName = "const" + constants.size();
        constants.put(constant, new Constant(constName, type));
    }

    public boolean isLibraryFunction(String name){
        for(Function function : this.libraryFunctions){
            if(function.name.equals(name)){
                return true;
            }
        }
        return false;
    }
    public boolean symbolExists(String name) {
        for(List<Symbol> symbols : this.localSymbolTable){
            for(Symbol symbol : symbols){
                if(symbol.name.equals(name)){
                    return true;
                }
            }
        }
        return false;
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
        Struct struct = lookupStruct(this.structs, structSymbol.type.name);
        for(StructField field : struct.fields){
            if(field.name.equals(member)){
               return new Symbol(member, field.type);
            }
        }
        throw new UnknownSymbolException(String.format("Tried to access member '%s' in struct '%s', doesnt exist", member, structSymbol.name));
    }

    public static Struct lookupStruct(List<Struct> structs, String name) throws UnknownSymbolException {
        for(Struct struct : structs){
            if(struct.type.name.equals(name)){
                return struct;
            }
        }
        throw new UnknownSymbolException(String.format("Couldn't find struct '%s'", name));
    }

    private void addLibraryFunctions(){
        List<StructField> mallocArgs = new ArrayList<>();
        mallocArgs.add(new StructField("size", DataType.getInt(), "int"));
        Function malloc = new Function("malloc", mallocArgs, DataType.getVoidPointer(), new QuadList());

        this.libraryFunctions.add(malloc);

        List<StructField> freeArgs = new ArrayList<>();
        freeArgs.add(new StructField("pointer", DataType.getVoidPointer(), "void *"));
        Function free = new Function("free", freeArgs, DataType.getVoid(), new QuadList());

        this.libraryFunctions.add(free);

        List<StructField> printArgs = new ArrayList<>();
        Function print = new Function("printf", printArgs, DataType.getVoid(), new QuadList());

        this.libraryFunctions.add(print);
    }

    public SymbolTable(){
        this.constants = new HashMap<>();
        this.structs = new ArrayList<>();
        this.functions = new ArrayList<>();
        this.libraryFunctions = new ArrayList<>();
        this.addLibraryFunctions();
        this.localSymbolTable = new java.util.Stack<>();
    }
}
