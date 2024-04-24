package se.liu.albhe576.project;

import java.util.*;

public class Scope {
    private final List<Scope> children;
    private final Map<String, VariableSymbol> variables;
    private boolean closed;
    public void closeScope(){this.closed = true;}
    public boolean isClosed(){return this.closed;}
    public Map<Integer, VariableSymbol> getAllScopedVariables(){
        Map<Integer, VariableSymbol> vars = new HashMap<>();
        for(VariableSymbol symbol : this.variables.values()){
            vars.put(symbol.id, symbol);
        }
        for(Scope child : children){
            vars.putAll(child.getAllScopedVariables());
        }
        return vars;
    }

    public Scope(Map<String, VariableSymbol> variables){
        this.children = new ArrayList<>();
        this.variables = variables;
        this.closed = false;
    }
    public Scope(){this(new HashMap<>());}
    public void addVariable(String name, VariableSymbol symbol){this.variables.put(name, symbol);}
    public List<Scope> getChildren(){return this.children;}
    public Scope getLastChild(){
        if(this.children.isEmpty()){
            return null;
        }
        return this.children.get(this.children.size() - 1);
    }
    public Collection<VariableSymbol> getVariables(){return this.variables.values();}
    public VariableSymbol getVariable(String name){return this.variables.get(name);}
    public boolean variableExists(String name){return this.variables.containsKey(name);}
    public void addChild(){this.children.add(new Scope());}

}
