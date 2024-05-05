package se.liu.albhe576.project.backend;


/**
 * A symbol is created within an intermediate and is defined by it's type.
 * The name is primarily used for either debugging purposes but also used to figure out where the data represented by the symbol is located
 * @see SymbolTable
 */
public class Symbol {
    @Override
    public String toString() {
        return String.format("[%s:%s]", name, type);
    }
    public boolean isNull(){
        return false;
    }
    protected final String name;
    protected final DataType type;
    public DataType getType(){
        return this.type;
    }
    public String getName(){
        return this.name;
    }
    public Symbol(String name, DataType type){
        this.name = name;
        this.type = type;
    }
}
