package se.liu.albhe576.project;

public class VariableSymbol extends Symbol {
    public final int offset;
    public VariableSymbol(String name, DataType type, int offset){
       super(name, type);
       this.offset = offset;
    }

}
