package se.liu.albhe576.project.backend;

/**
 * A symbol that has an offset from the base pointer where it lives
 * @see Symbol
 * @see SymbolTable
 * @see Scope
 */
public class VariableSymbol extends Symbol {
    private final int offset;
    public int getOffset(){
        return this.offset;
    }
    public VariableSymbol(String name, DataType type, int offset){
       super(name, type);
       this.offset = offset;
    }

}
