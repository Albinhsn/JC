package se.liu.albhe576.project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionStmt extends Stmt{

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

    private final DataType returnType;
    private final String name;
    private final List<StructField> arguments;
    private final List<Stmt> body;

    public FunctionStmt(DataType returnType, String name, List<StructField> arguments, List<Stmt> body, int line, String file){
        super(line, file);
        this.returnType = returnType;
        this.name = name;
        this.arguments = arguments;
        this.body = body;

    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException{
        symbolTable.addFunction(new Function(name, arguments, returnType, quads));

        Map<String, VariableSymbol> localSymbols =new HashMap<>();
        int offset = 16;
        for(StructField arg : arguments){
           localSymbols.put(arg.name, new VariableSymbol(arg.name, arg.type, offset, 0));
           offset += symbolTable.getStructSize(arg.type);
        }

        symbolTable.compileFunction(name, localSymbols);
        symbolTable.enterScope();
        for(Stmt stmt : body){
            stmt.compile(symbolTable, quads);
        }
        symbolTable.exitScope();
    }
}
