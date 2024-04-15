package se.liu.albhe576.project;

public class VariableSymbol extends Symbol{
    public StructSymbol type;
    public static boolean isStruct(String name){
        switch(name){
            case "float":{}
            case "byte":{}
            case "int":{
                return false;
            }
        }
        return true;
    }
    public VariableSymbol(StructSymbol type, String name) {
        super(name);
        this.type = type;
    }
}
