package se.liu.albhe576.project;

public class ImmediateSymbol extends Symbol{

    @Override
    public String toString() {
        return value;
    }

    public String value;
    public ImmediateSymbol(String name, DataType type, String value){
        super(name, type);
        this.value = value;
    }
}
