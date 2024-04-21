package se.liu.albhe576.project;

import java.util.List;

public class Function {

    private final int line;
    private final String file;
    public final boolean external;
    private final List<StructField> arguments;
    private final DataType returnType;
    private final QuadList intermediates;

    public List<StructField> getArguments(){
        return this.arguments;
    }
    public int getLine(){
        return this.line;
    }
    public String getFile(){
        return this.file;
    }
    public DataType getReturnType(){
        return this.returnType;
    }
    public QuadList getIntermediates(){
        return this.intermediates;
    }
    public Symbol getFunctionSymbol(String name){
        return new Symbol(name, returnType);
    }
    public Function(List<StructField> arguments, DataType returnType, QuadList intermediates, String file, int line, boolean external){
        this.arguments = arguments;
        this.returnType = returnType;
        this.intermediates = intermediates;
        this.file = file;
        this.line = line;
        this.external = external;
    }
    public Function(DataType returnType, QuadList intermediates, String file, int line){
        this(null, returnType,intermediates, file,line, true);
    }
}
