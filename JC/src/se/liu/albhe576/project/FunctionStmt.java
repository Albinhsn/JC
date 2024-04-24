package se.liu.albhe576.project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionStmt extends Stmt{

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
        if(symbolTable.functionExists(name)){
            Function declaredFunction = symbolTable.getFunction(name);
            Compiler.error(String.format("Trying to redeclare function %s in %s:%d, already declared at %s:%d", name, file, line, declaredFunction.getFile(), declaredFunction.getLine()), line, file);
        }

        symbolTable.addFunction(name, new Function(arguments, returnType, quads, file, line, false));

        Map<String, VariableSymbol> localSymbols =new HashMap<>();

        // is for ip and bp that will be pushed after call
        int offset = 16;
        for(StructField arg : arguments){
           localSymbols.put(arg.name(), new VariableSymbol(arg.name(), arg.type(), offset, symbolTable.generateVariableId()));
           offset += SymbolTable.getStructSize(symbolTable.getStructs(), arg.type());
        }

        symbolTable.compileFunction(name, localSymbols);
        Stmt.compileBlock(symbolTable, quads, body);
    }
}
