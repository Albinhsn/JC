package se.liu.albhe576.project;

import java.util.List;

public class Function {

    public String name;
    public List<StructField> arguments;
    public DataType returnType;
    public List<Quad> intermediates;

    public Symbol getFunctionSymbol(){
        return new Symbol(name, DataType.getFunction());
    }
    public Function(String name, List<StructField> arguments, DataType returnType, List<Quad> intermediates){
        this.name = name;
        this.arguments = arguments;
        this.returnType = returnType;
        this.intermediates = intermediates;
    }
}
