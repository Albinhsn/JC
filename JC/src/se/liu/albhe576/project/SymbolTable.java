package se.liu.albhe576.project;

import java.util.*;

public class SymbolTable {

    private final List<Function> extern;
    public final Map<String, Struct> structs;
    private final List<Function> functions;
    private final Map<String, Constant> constants;
    private final Map<String, Map<String, VariableSymbol>> localSymbolTable;
    private int scopeDepth;

    public void enterScope(){
        this.scopeDepth++;
    }
    public void exitScope(){
        this.scopeDepth--;
    }
    public void compileFunction(String name, Map<String, VariableSymbol> arguments){
        this.localSymbolTable.put(name, arguments);
    }

    public boolean isDeclaredStruct(String name){
        final String[] internal = new String[]{
                "int",
                "float",
                "string",
        };
        if(Arrays.asList(internal).contains(name)){
            return true;
        }
        return this.structs.containsKey(name);
    }

    public int getStructSize(DataType type){
        if(this.structs.containsKey(type.name) && type.isStruct()){
            return this.structs.get(type.name).getSize(this.structs);
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
        int offset = -this.getStructSize(type) - this.getLocalVariableStackSize(getCurrentFunction().name);
        VariableSymbol variableSymbol = new VariableSymbol(name, type, offset, scopeDepth);
        getCurrentLocals().put(name, variableSymbol);
        return variableSymbol;
    }
    public void addSymbol(VariableSymbol symbol){
        getCurrentLocals().put(symbol.name, symbol);
    }

    public int getLocalVariableStackSize(String name){
        Map<String, VariableSymbol> locals = getLocals(name);
        int size = 0;
        for(VariableSymbol variableSymbol : locals.values()){
            size = Math.min(variableSymbol.offset, size);
        }
        return -size;
    }



    public boolean functionExists(String name){
        for(Function function : functions){
            if(function.name.equals(name)){
                return true;
            }
        }
        for(Function function : extern){
            if(function.name.equals(name)){
                return true;
            }
        }
        return false;
    }

    public int getCurrentScopeSize(){
        return this.getLocalVariableStackSize(this.getCurrentFunction().name);
    }
    public int getDepth(){
        return this.scopeDepth;
    }
    public void addFunction(Function function){
        this.functions.add(function);
    }
    public Function getFunction(String name) throws CompileException{
        for(Function function : functions){
            if(function.name.equals(name)){
                return function;
            }
        }
        for(Function function : extern){
            if(function.name.equals(name)){
                return function;
            }
        }
        throw new CompileException(String.format("Can't find function %s", name));
    }

    public void addConstant(String constant, DataTypes type){
        if(constants.containsKey(constant)){
            return;
        }
        String constName = "const" + constants.size();
        constants.put(constant, new Constant(constName, type));
    }

    public boolean isExternFunction(String name){
        for(Function function : extern){
            if(function.name.equals(name)){
                return true;
            }
        }
        return false;
    }
    public Function getExternFunction(String name) throws CompileException{
        for(Function function : extern){
            if(function.name.equals(name)){
                return function;
            }
        }
        throw new CompileException(String.format("Can't find extern function %s", name));
    }
    public boolean symbolExists(String name) {
        if(!this.getCurrentLocals().containsKey(name)){
           return false;
        }
        return this.getCurrentLocals().get(name).depth >= this.scopeDepth;
    }
    public Symbol findSymbol(String name) {
        return this.getCurrentLocals().get(name);
    }
    public boolean isMemberOfStruct(DataType type, String member) {
        Struct struct = this.structs.get(type.name);
        for(StructField field : struct.fields){
            if(field.name.equals(member)){
                return true;
            }
        }
        return false;
    }
    public Symbol getMemberSymbol(Symbol structSymbol, String member) throws CompileException {
        Struct struct = this.structs.get(structSymbol.type.name);
        for(StructField field : struct.fields){
            if(field.name.equals(member)){
               return new Symbol(member, field.type);
            }
        }
        throw new CompileException(String.format("Tried to access member '%s' in struct '%s', doesnt exist", member, structSymbol.name));
    }

    public SymbolTable(Map<String, Struct> structs, Map<String, Constant> constants, List<Function> extern){
        this.scopeDepth = 0;
        this.structs = structs;
        this.extern = extern;
        this.constants =constants;
        this.functions = new ArrayList<>();
        this.localSymbolTable = new HashMap<>();
    }
}
