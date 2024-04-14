package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        List<Quad> out = new ArrayList<>();

        symbolTable.peek().add(new FunctionSymbol(name, arguments, returnType));
        List<Symbol> functionSymbols = new ArrayList<>();
        for(StructField arg : arguments){
            functionSymbols.add(new VariableSymbol(arg.type, arg.name));
        }

        symbolTable.push(functionSymbols);
        for(Stmt stmt : body){
            out.addAll(stmt.compile(symbolTable));
        }
        symbolTable.pop();

        return out;
    }
}
