package se.liu.albhe576.project.backend;

import java.util.*;

/**
 *
 * A scope is block that has it's own defined variables that can be used in itself and it's children
 * When defining a function creates a top scope (since there is no global variables) and when entering a new block creates a new child scope
 * When exiting a scope it becomes closed which means it variables cannot be reached.
 * @see SymbolTable
 */
public class Scope implements Iterator<Scope>{

    @Override
    public boolean hasNext() {
        return !(this.children.isEmpty() || this.getLastChild().isClosed());
    }
    @Override
    public Scope next() throws NoSuchElementException{
        Scope lastChild = getLastChild();
        if(lastChild == null){
            throw new NoSuchElementException("Scope has no next element!");
        }
        return getLastChild();
    }

    private final List<Scope> children;
    private final Map<String, VariableSymbol> variables;
    private boolean closed;
    public void closeScope(){this.closed = true;}
    public boolean isClosed(){return this.closed;}
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
