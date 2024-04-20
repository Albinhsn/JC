package se.liu.albhe576.project;

import java.util.List;

public class Function {

    public String name;
    public List<StructField> arguments;
    public DataType returnType;
    public QuadList intermediates;
    public final boolean varArgs;
    public Symbol getFunctionSymbol(){
        return new Symbol(name, returnType);
    }
    public Function(String name, List<StructField> arguments, DataType returnType, QuadList intermediates){
        this.name = name;
        this.arguments = arguments;
        this.returnType = returnType;
        this.intermediates = intermediates;
        this.varArgs = false;
    }
    public Function(String name, DataType returnType, QuadList intermediates){
        this.name = name;
        this.arguments = null;
        this.returnType = returnType;
        this.intermediates = intermediates;
        this.varArgs = true;
    }
}
