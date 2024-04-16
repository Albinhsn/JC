package se.liu.albhe576.project;


public class Symbol {

    @Override
    public String toString() {
        return name;
    }

    public boolean isNull(){
        return false;
    }

    public String name;
    public DataType type;
    public Symbol(String name, DataType type){
        this.name = name;
        this.type = type;
    }
}
