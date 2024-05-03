package se.liu.albhe576.project;

import java.util.*;
import java.util.Stack;

public class SymbolTable {

    private final Map<String, Struct> structs;
    private final Map<String, Function> functions;
    private final Map<String, Constant> constants;
    private final Map<String, Scope> scopes;
    private String currentFunctionName;
    private int resultCount;
    private int labelCount;
    public Symbol generateSymbol(DataType type){
        return new Symbol("T" + resultCount++, type);
    }
    public ImmediateSymbol generateImmediateSymbol(DataType type, String literal){return new ImmediateSymbol("T" + resultCount++, type, literal);}
    public Symbol generateLabel(){return new Symbol( String.format("label%d", labelCount++), new DataType("label", DataTypes.VOID, 0));}
    private static final String[] internalStructs = new String[]{"int", "float", "byte", "string", "short", "double", "long"};
    public void addFunction(String name, Map<String, VariableSymbol> arguments){
        this.scopes.put(name, new Scope(arguments));
        this.currentFunctionName = name;
    }
    public Map<String, Struct> getStructs(){return this.structs;}

    public boolean isDeclaredStruct(String name){
        if(Arrays.asList(SymbolTable.internalStructs).contains(name)){
            return true;
        }
        return this.structs.containsKey(name);
    }

    public static int getStructSize(Map<String, Struct> structs, DataType type){
        if(structs.containsKey(type.name) && type.isStruct()){
            return structs.get(type.name).getSize(structs);
        }
        return type.getSize();
    }
    public int getStructSize(DataType type){
        if(structs.containsKey(type.name) && type.isStruct()){
            return structs.get(type.name).getSize(structs);
        }
        return type.getSize();
    }
    public DataType getCurrentFunctionReturnType(){return this.functions.get(this.currentFunctionName).getReturnType();}
    public String getCurrentFunctionName(){
        return this.currentFunctionName;
    }
    private Scope getCurrentScope(){
        Scope scope = this.scopes.get(this.currentFunctionName);
        while(scope.hasNext()){
            scope = scope.next();
        }
        return scope;
    }
    public Map<String, Function> getExternalFunctions(){
        Map<String, Function> out = new HashMap<>();
        for(Map.Entry<String, Function> entry : this.functions.entrySet()){
            if(entry.getValue().isExternal()){
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }
    public Map<String, Function> getInternalFunctions(){
        Map<String, Function> out = new HashMap<>();
        for(Map.Entry<String, Function> entry : this.functions.entrySet()){
            if(!entry.getValue().isExternal()){
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }
    public Map<String, Constant> getConstants(){
        return this.constants;
    }
    public void enterScope(){
        Scope lastScope = this.getCurrentScope();
        lastScope.addChild();
    }
    public void exitScope(){this.getCurrentScope().closeScope();}

    public VariableSymbol addVariable(String name, DataType type){
        int offset = -1 * (SymbolTable.getStructSize(structs, type) + this.getLocalVariableStackSize());
        VariableSymbol variableSymbol = new VariableSymbol(name, type, offset);
        getCurrentScope().addVariable(name, variableSymbol);
        return variableSymbol;
    }
    public void addVariable(VariableSymbol symbol){
        getCurrentScope().addVariable(symbol.name, symbol);
    }
    public int getLocalVariableStackSize(){return getLocalVariableStackSize(this.currentFunctionName);}
    public int getLocalVariableStackSize(String name){
        int size                        = 0;
        Scope outerScope                = this.scopes.get(name);
        java.util.Stack<Scope> scopes   = new Stack<>();
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

    public Function getFunction(String name) throws CompileException{
        if(!this.functions.containsKey(name)){
            throw new CompileException(String.format("Can't find function %s", name));
        }
        return this.functions.get(name);
    }

    public void addConstant(String constant, DataTypes type){
        if(constants.containsKey(constant)){
            return;
        }
        final String constName = "const" + constants.size();
        constants.put(constant, new Constant(constName, type));
    }
    public boolean symbolExists(String name) {return findSymbol(name) != null;}
    public VariableSymbol findSymbol(String name){
        Scope scope = this.scopes.get(this.currentFunctionName);
        while(true){
            if(scope.variableExists(name)){
                return scope.getVariable(name);
            }
            if(!scope.hasNext()){
                return null;
            }
            scope = scope.next();
        }
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
    public void addArguments(Map<String, VariableSymbol> localSymbols, Function function){
        // IP and BP
        int offset = 16;
        for(StructField arg : function.getArguments()){
            localSymbols.put(arg.name(), new VariableSymbol(arg.name(), arg.type(), offset));
            offset += getStructSize(arg.type());
        }

    }
    public SymbolTable(Map<String, Struct> structs, Map<String, Function> extern){
        this.structs = structs;
        this.constants = new HashMap<>();
        this.functions = new HashMap<>(extern);
        this.scopes = new HashMap<>();
    }
}
