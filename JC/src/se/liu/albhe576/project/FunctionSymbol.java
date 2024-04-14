package se.liu.albhe576.project;

import java.util.List;

public class FunctionSymbol extends Symbol {

    List<StructField> arguments;
    StructType returnType;
    public FunctionSymbol(String name, List<StructField> arguments, StructType returnType){
        super(name);
        this.arguments = arguments;
        this.returnType = returnType;
    }
}
