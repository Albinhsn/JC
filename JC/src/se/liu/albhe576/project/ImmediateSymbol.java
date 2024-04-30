package se.liu.albhe576.project;

public class ImmediateSymbol extends Symbol{
    private final String value;
    @Override
    public String toString() {
        return "[" + value + "]";
    }
    @Override
    public boolean isNull() {
        return type.type == DataTypes.INT && value.equals("0");
    }
    public String getValue(){
        return value;
    }
    public ImmediateSymbol(String name, DataType type, String value){
        super(name, type);
        this.value = value;
    }
}
