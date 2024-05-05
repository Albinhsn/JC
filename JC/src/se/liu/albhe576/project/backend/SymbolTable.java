package se.liu.albhe576.project.backend;

import java.util.*;
import se.liu.albhe576.project.frontend.*;

/**
 * Contains every variable, constant, function and the definition of them and structures.
 * Used to figure out what every symbol refers to and where it is located.
 * @see Symbol
 * @see Constant
 * @see Function
 * @see VariableSymbol
 */
public class SymbolTable {

    private final Map<String, Structure> structures;
    private final Map<String, Function> functions;
    private final Map<String, Constant> constants;
    private final Map<String, Scope> scopes;
    private String currentFunctionName;
    private int resultCount;
    private int labelCount;
    private static final String[] INTERNAL_STRUCTURES = new String[]{"int", "float", "byte", "string", "short", "double", "long"};
    public Symbol generateSymbol(DataType type){
	Symbol symbol = new Symbol("T" + resultCount, type);
	resultCount++;
	return symbol;
    }
    public ImmediateSymbol generateImmediateSymbol(DataType type, String literal){
	ImmediateSymbol immediateSymbol = new ImmediateSymbol("T" + resultCount, type, literal);
	resultCount++;
	return immediateSymbol;
    }
    public Symbol generateLabel(){
	Symbol symbol = new Symbol(String.format("label%d", labelCount), new DataType("label", DataTypes.VOID, 0));
	labelCount++;
	return symbol;
    }
    public void addFunction(String name, Map<String, VariableSymbol> arguments){
        this.scopes.put(name, new Scope(arguments));
        this.currentFunctionName = name;
    }
    public Map<String, Structure> getStructures(){return this.structures;}

    public boolean isDeclaredStruct(String name){
        if(Arrays.asList(SymbolTable.INTERNAL_STRUCTURES).contains(name)){
            return true;
        }
        return this.structures.containsKey(name);
    }

    public static int getStructureSize(Map<String, Structure> structures, DataType type){
        if(structures.containsKey(type.name) && type.isStructure()){
            return structures.get(type.name).getSize(structures);
        }
        return type.getSize();
    }
    public int getStructureSize(DataType type){
        if(structures.containsKey(type.name) && type.isStructure()){
            return structures.get(type.name).getSize(structures);
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
    public boolean functionIsExternal(String name){
        return this.functions.get(name).isExternal();
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
        int offset = -1 * (SymbolTable.getStructureSize(structures, type) + this.getLocalVariableStackSize());
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
        ArrayDeque<Scope> scopes   = new ArrayDeque<>();
        scopes.add(outerScope);

        while(!scopes.isEmpty()){
            Scope scope = scopes.pop();
            for(VariableSymbol variableSymbol : scope.getVariables()){
                size = Math.min(variableSymbol.getOffset(), size);
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
        Structure structure = this.structures.get(type.name);
        if(structure == null){
            throw new CompileException(String.format("Can't find member of non existing struct %s", type.name));
        }
        for(StructureField field : structure.getFields()){
            if(field.name().equals(member)){
                return true;
            }
        }
        return false;
    }
    public void addArguments(Map<String, VariableSymbol> localSymbols, Function function){
        // IP and BP
        int offset = 16;
        for(StructureField arg : function.getArguments()){
            localSymbols.put(arg.name(), new VariableSymbol(arg.name(), arg.type(), offset));
            offset += getStructureSize(arg.type());
        }

    }
    public SymbolTable(Map<String, Structure> structures, Map<String, Function> extern){
        this.structures = structures;
        this.constants = new HashMap<>();
        this.functions = new HashMap<>(extern);
        this.scopes = new HashMap<>();
        this.currentFunctionName = null;
    }
}
