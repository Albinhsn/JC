package se.liu.albhe576.project;


public class Symbol {
    @Override
    public String toString() {
        return String.format("[%s:%s]", name, type.toString());
    }
    public boolean isNull(){
        return false;
    }
    protected final String name;
    protected final DataType type;
    public Symbol(String name, DataType type){
        this.name = name;
        this.type = type;
    }
}
