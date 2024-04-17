package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

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

    public FunctionStmt(DataType returnType, String name, List<StructField> arguments, List<Stmt> body, int line){
        super(line);
        this.returnType = returnType;
        this.name = name;
        this.arguments = arguments;
        this.body = body;

    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        QuadList out = new QuadList();
        symbolTable.addFunction(new Function(name, arguments, returnType, out));

        List<Symbol> localSymbols =new ArrayList<>();
        for(StructField arg : arguments){
           localSymbols.add(new Symbol(arg.name, arg.type));
        }

        symbolTable.enterScope(localSymbols);
        for(Stmt stmt : body){
            out.concat(stmt.compile(symbolTable));
        }
        symbolTable.exitScope();

        return out;
    }
}
