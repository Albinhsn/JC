package se.liu.albhe576.project;

import java.util.*;
import java.util.Stack;

public class SymbolTable {

    private final Map<String, Struct> structs;
    private final Map<String, Function> functions;
    private final Map<String, Constant> constants;
    private final Map<String, Scope> scopes;
    private String currentFunctionName;
    private int variableCount;
    public void compileFunction(String name, Map<String, VariableSymbol> arguments){
        this.scopes.put(name, new Scope(arguments));
        this.currentFunctionName = name;
    }
    public Map<String, Struct> getStructs(){return this.structs;}
    public Struct getStruct(String name){return this.structs.get(name);}
    public Map<Integer, VariableSymbol> getAllScopedVariables(String functionName){
        return this.scopes.get(functionName).getAllScopedVariables();
    }

    public boolean isDeclaredStruct(String name){
        final String[] internal = new String[]{
                "int",
                "float",
                "byte",
                "string",
        };
        if(Arrays.asList(internal).contains(name)){
            return true;
        }
        return this.structs.containsKey(name);
    }

    public static int getStructSize(Map<String, Struct> structs, DataType type){
        if(structs.containsKey(type.name) && type.isStruct()){
            return structs.get(type.name).getSize(structs);
        }else if(type.isByte()){
            return 1;
        }
        return 8;
    }
    public Function getCurrentFunction(){
        return this.functions.get(this.currentFunctionName);
    }
    public String getCurrentFunctionName(){
        return this.currentFunctionName;
    }
    private Scope getCurrentScope(){
        Scope curr = this.scopes.get(this.currentFunctionName);
        while(true){
            List<Scope> children = curr.getChildren();
            if(children.isEmpty() || children.get(children.size() - 1).isClosed()){
                return curr;
            }
            curr = children.get(children.size() - 1);
        }
    }
    public Map<String, Function> getInternalFunctions(){
        Map<String, Function> out = new HashMap<>();
        for(Map.Entry<String, Function> entry : this.functions.entrySet()){
            if(!entry.getValue().external){
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }
    public Map<String, Function> getExternalFunctions(){
        Map<String, Function> out = new HashMap<>();
        for(Map.Entry<String, Function> entry : this.functions.entrySet()){
            if(entry.getValue().external){
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }
    public Map<String, Constant> getConstants(){
        return this.constants;
    }
    public int generateVariableId(){
        return this.variableCount++;
    }

    public void enterScope(){
        Scope lastScope = this.getCurrentScope();
        lastScope.addChild();
    }
    public void exitScope(){this.getCurrentScope().closeScope();}

    public VariableSymbol addVariable(String name, DataType type){
        int offset = -SymbolTable.getStructSize(structs, type) - this.getLocalVariableStackSize(this.currentFunctionName);
        VariableSymbol variableSymbol = new VariableSymbol(name, type, offset, this.generateVariableId());
        getCurrentScope().addVariable(name, variableSymbol);
        return variableSymbol;
    }
    public void addVariable(VariableSymbol symbol){
        getCurrentScope().addVariable(symbol.getName(), symbol);
    }

    public int getLocalVariableStackSize(String name){
        int size = 0;
        Scope outerScope = this.scopes.get(name);
        java.util.Stack<Scope> scopes = new Stack<>();
        scopes.add(outerScope);

        while(!scopes.empty()){
            Scope scope = scopes.pop();
            for(VariableSymbol variableSymbol : scope.getVariables()){
                size = Math.min(variableSymbol.offset, size);
            }
            scopes.addAll(scope.getChildren());
        }

        return -size;
    }

    public boolean functionExists(String name){
        return this.functions.containsKey(name);
    }

    public int getCurrentScopeSize(){
        return this.getLocalVariableStackSize(currentFunctionName);
    }
    public void addFunction(String name, Function function){
        this.functions.put(name, function);
        this.currentFunctionName = name;
    }
    public Function getFunction(String name) throws CompileException{
        if(!this.functions.containsKey(name)){
            throw new CompileException(String.format("Can't find function %s", name));
        }
        return this.functions.get(name);
    }
    public Map<String, Function> getFunctions() {
        return this.functions;
    }

    public void addConstant(String constant, DataTypes type){
        if(constants.containsKey(constant)){
            return;
        }
        String constName = "const" + constants.size();
        constants.put(constant, new Constant(constName, type));
    }

    public boolean isExternFunction(String name){
        return this.functions.get(name).external;
    }
    public boolean symbolExists(String name) {
        Scope scope = this.scopes.get(this.currentFunctionName);

        while(true){
            if(scope.variableExists(name)){
               return true;
            }
            if(scope.getChildren().isEmpty()){
                return false;
            }
            scope = scope.getLastChild();
            if(scope.isClosed()){
                return false;
            }
        }
    }
    public Symbol findSymbol(String name){
        Scope scope = this.scopes.get(this.currentFunctionName);
        java.util.Stack<Scope> scopes = new Stack<>();
        scopes.add(scope);

        while(!scopes.empty()){
            if(scope.variableExists(name)){
                return scope.getVariable(name);
            }
            scopes.addAll(scope.getChildren());
            scope = scopes.pop();
        }
        return null;
    }
    public boolean isMemberOfStruct(DataType type, String member) throws CompileException {
        Struct struct = this.structs.get(type.name);
        if(struct == null){
            throw new CompileException(String.format("Can't find member of non existing struct %s", type.name));
        }
        for(StructField field : struct.getFields()){
            if(field.name().equals(member)){
                return true;
            }
        }
        return false;
    }
    public Symbol getMemberSymbol(Symbol structSymbol, String member) throws CompileException {
        Struct struct = this.structs.get(structSymbol.getType().name);
        for(StructField field : struct.getFields()){
            if(field.name().equals(member)){
               return new Symbol(member, field.type());
            }
        }
        throw new CompileException(String.format("Tried to access member '%s' in struct '%s', doesnt exist", member, structSymbol.getName()));
    }

    public SymbolTable(Map<String, Struct> structs, Map<String, Constant> constants, Map<String, Function> extern){
        this.structs = structs;
        this.constants =constants;
        this.functions = new HashMap<>(extern);
        this.scopes = new HashMap<>();
        this.variableCount = 0;
    }
}
