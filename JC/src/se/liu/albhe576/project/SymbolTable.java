package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    public final Map<String, Struct> structs;
    private final List<Function> functions;
    private final Map<String, Function> libraryFunctions;
    private final Map<String, Constant> constants;
    private final Map<String, Map<String, VariableSymbol>> localSymbolTable;
    private int scopeDepth;

    public void enterScope(){
        this.scopeDepth++;
    }
    public void exitScope(){
        this.scopeDepth--;
    }
    public void compileFunction(String name){
        this.localSymbolTable.put(name, new HashMap<>());
    }
    public void compileFunction(String name, Map<String, VariableSymbol> arguments){
        this.localSymbolTable.put(name, arguments);
    }

    public int getStructSize(String name){
        // ToDo :)
        if(this.structs.containsKey(name)){
            return this.structs.get(name).getSize(this.structs);
        }
        return 8;
    }
    public Function getCurrentFunction(){
        return this.functions.get(this.functions.size() - 1);
    }
    public Map<String, VariableSymbol> getLocals(String name){
        return this.localSymbolTable.get(name);
    }
    private Map<String, VariableSymbol> getCurrentLocals(){
        return this.localSymbolTable.get(this.getCurrentFunction().name);
    }
    public List<Function> getFunctions(){
        return this.functions;
    }
    public Map<String, Constant> getConstants(){
        return this.constants;
    }
    public VariableSymbol addSymbol(String name, DataType type){
        int offset = -this.getStructSize(type.name) - this.getScopeSize(getCurrentFunction().name);
        VariableSymbol variableSymbol = new VariableSymbol(name, type, offset, scopeDepth);
        getCurrentLocals().put(name, variableSymbol);
        return variableSymbol;
    }
    public VariableSymbol addSymbol(VariableSymbol symbol){
        getCurrentLocals().put(symbol.name, symbol);
        return symbol;
    }

    public int getScopeSize(String name){
        Map<String, VariableSymbol> locals = getLocals(name);
        int size = 0;
        for(VariableSymbol variableSymbol : locals.values()){
            size = Math.min(variableSymbol.offset, size);
        }
        return -size;
    }

    public int getCurrentScopeSize(){
        return this.getScopeSize(this.getCurrentFunction().name);
    }
    public int getDepth(){
        return this.scopeDepth;
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
        throw new UnknownSymbolException(String.format("Can't find function %s", name));
    }
    public Function getLibraryFunction(String name) throws UnknownSymbolException{
        return this.libraryFunctions.get(name);
    }

    public void addConstant(String constant, DataTypes type){
        if(constants.containsKey(constant)){
            return;
        }
        String constName = "const" + constants.size();
        constants.put(constant, new Constant(constName, type));
    }

    public boolean isLibraryFunction(String name){
        return this.libraryFunctions.containsKey(name);
    }
    public boolean symbolExists(String name) {
        return this.getCurrentLocals().containsKey(name);
    }
    public Symbol findSymbol(String name) {
        return this.getCurrentLocals().get(name);
    }
    public Symbol getMemberSymbol(Symbol structSymbol, String member) throws UnknownSymbolException {
        Struct struct = this.structs.get(structSymbol.type.name);
        for(StructField field : struct.fields){
            if(field.name.equals(member)){
               return new Symbol(member, field.type);
            }
        }
        throw new UnknownSymbolException(String.format("Tried to access member '%s' in struct '%s', doesnt exist", member, structSymbol.name));
    }
    private void addLibraryFunctions(){
        List<StructField> mallocArgs = new ArrayList<>();
        mallocArgs.add(new StructField("size", DataType.getInt(), "int"));
        Function malloc = new Function("malloc", mallocArgs, DataType.getVoidPointer(), new QuadList());

        this.libraryFunctions.put("malloc", malloc);

        List<StructField> freeArgs = new ArrayList<>();
        freeArgs.add(new StructField("pointer", DataType.getVoidPointer(), "void *"));
        Function free = new Function("free", freeArgs, DataType.getVoid(), new QuadList());

        this.libraryFunctions.put("free", free);

        List<StructField> printArgs = new ArrayList<>();
        Function print = new Function("printf", printArgs, DataType.getVoid(), new QuadList());

        this.libraryFunctions.put("printf",print);
    }

    public SymbolTable(Map<String, Constant> constants){
        this.scopeDepth = 0;
        this.structs = new HashMap<>();
        this.constants =constants;
        this.functions = new ArrayList<>();
        this.libraryFunctions = new HashMap<>();
        this.addLibraryFunctions();
        this.localSymbolTable = new HashMap<>();
    }
}
