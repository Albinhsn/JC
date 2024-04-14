package se.liu.albhe576.project;

import java.util.List;

public class FunctionStmt implements Stmt{

    @Override
    public String toString() {
       StringBuilder s = new StringBuilder();
       s.append(String.format("%s %s(", returnType, name));
       for(int i = 0; i < arguments.size(); i++){
          s.append(arguments.get(i));
          if(i != arguments.size() - 1){
              s.append(", ");
          }
       }
       s.append("){\n");
       for(Stmt stmt : body){
           s.append(String.format("\t%s\n", stmt));
       }
       s.append("}\n");
       return s.toString();

    }

    private final StructType returnType;
    private final String name;
    private final List<StructField> arguments;
    private final List<Stmt> body;

    public FunctionStmt(StructType returnType, String name, List<StructField> arguments, List<Stmt> body){
        this.returnType = returnType;
        this.name = name;
        this.arguments = arguments;
        this.body = body;

    }
}
