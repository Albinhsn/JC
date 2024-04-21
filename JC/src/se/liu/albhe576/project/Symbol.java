package se.liu.albhe576.project;


public class Symbol {

    @Override
    public String toString() {
        return name;
    }

    public boolean isNull(){
        return false;
    }

    protected final String name;
    protected final DataType type;

    public String getName(){
        return this.name;
    }
    public DataType getType(){
        return this.type;
    }
    public Symbol(String name, DataType type){
        this.name = name;
        this.type = type;
    }
}
