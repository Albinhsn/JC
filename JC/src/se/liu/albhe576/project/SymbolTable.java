package se.liu.albhe576.project;

import java.util.*;
import java.util.Stack;

public class SymbolTable {

    private final List<Function> extern;
    public final Map<String, Struct> structs;
    private final List<Function> functions;
    private final Map<String, Constant> constants;
    private final Map<String, Scope> scopes;
    private int variableCount;
    public void compileFunction(String name, Map<String, VariableSymbol> arguments){
        this.scopes.put(name, new Scope(arguments));
    }
    public Map<Integer, VariableSymbol> getAllScopedVariables(String functionName){
        return this.scopes.get(functionName).getAllScopedVariables();
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
    private Scope getCurrentScope(){
        Scope curr = this.scopes.get(this.getCurrentFunction().name);
        while(true){
            List<Scope> children = curr.getChildren();
            if(children.isEmpty() || children.get(children.size() - 1).closed){
                return curr;
            }
            curr = children.get(children.size() - 1);
        }
    }
    public List<Function> getFunctions(){
        return this.functions;
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
    public void exitScope(){
        this.getCurrentScope().closed = true;
    }

    public VariableSymbol addVariable(String name, DataType type){
        int offset = -this.getStructSize(type) - this.getLocalVariableStackSize(getCurrentFunction().name);
        VariableSymbol variableSymbol = new VariableSymbol(name, type, offset, this.generateVariableId());
        getCurrentScope().addVariable(name, variableSymbol);
        return variableSymbol;
    }
    public void addVariable(VariableSymbol symbol){
        getCurrentScope().addVariable(symbol.name, symbol);
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
        Scope scope = this.scopes.get(this.getCurrentFunction().name);

        while(true){
            if(scope.variableExists(name)){
               return true;
            }
            if(scope.getChildren().isEmpty()){
                return false;
            }
            scope = scope.getLastChild();
            if(scope.closed){
                return false;
            }
        }
    }
    public Symbol findSymbol(String name) throws CompileException{
        Scope scope = this.scopes.get(this.getCurrentFunction().name);
        java.util.Stack<Scope> scopes = new Stack<>();
        scopes.add(scope);

        while(!scopes.empty()){
            if(scope.variableExists(name)){
                return scope.getVariable(name);
            }
            scopes.addAll(scope.getChildren());
            scope = scopes.pop();
        }
        throw new CompileException(String.format("Couldn't find symbol %s", name));
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
        this.structs = structs;
        this.extern = extern;
        this.constants =constants;
        this.functions = new ArrayList<>();
        this.scopes = new HashMap<>();
        this.variableCount = 0;
    }
}
