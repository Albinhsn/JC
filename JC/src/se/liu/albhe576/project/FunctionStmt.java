package se.liu.albhe576.project;

import java.util.List;

public class FunctionStmt extends Stmt{

    private final VariableType returnType;
    private final String name;
    private final List<StructField> arguments;
    private final List<Stmt> body;

    public FunctionStmt(VariableType returnType, String name, List<StructField> arguments, List<Stmt> body){
        this.returnType = returnType;
        this.name = name;
        this.arguments = arguments;
        this.body = body;

    }
}
