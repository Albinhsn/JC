package se.liu.albhe576.project;

import java.util.List;

public class Function {

    private final int line;
    private final String file;
    public final boolean external;
    private final List<StructField> arguments;
    private final List<Stmt> body;
    private final DataType returnType;
    public List<StructField> getArguments(){return this.arguments;}
    public int getLine(){return this.line;}
    public String getFile(){return this.file;}
    public List<Stmt> getBody(){return this.body;}
    public DataType getReturnType(){return this.returnType;}
    public Symbol getFunctionSymbol(String name){return new Symbol(name, returnType);}
    public Function(List<StructField> arguments, DataType returnType, List<Stmt> stmts, String file, int line, boolean external){
        this.arguments = arguments;
        this.returnType = returnType;
        this.body = stmts;
        this.file = file;
        this.line = line;
        this.external = external;
    }
}
