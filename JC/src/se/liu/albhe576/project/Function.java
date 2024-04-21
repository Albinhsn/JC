package se.liu.albhe576.project;

import java.util.List;

public class Function {

    public final int line;
    public final String file;

    public String name;
    public List<StructField> arguments;
    public DataType returnType;
    public QuadList intermediates;
    public Symbol getFunctionSymbol(){
        return new Symbol(name, returnType);
    }
    public Function(String name, List<StructField> arguments, DataType returnType, QuadList intermediates, String file, int line){
        this.name = name;
        this.arguments = arguments;
        this.returnType = returnType;
        this.intermediates = intermediates;
        this.file = file;
        this.line = line;
    }
    public Function(String name, DataType returnType, QuadList intermediates, String file, int line){
        this(name, null, returnType,intermediates, file,line);
    }
}
