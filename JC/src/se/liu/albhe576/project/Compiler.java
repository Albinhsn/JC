package se.liu.albhe576.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Compiler {

    private final List<Stmt> stmts;

    private final List<List<Quad>> intermediate;
    private final Stack<List<Symbol>> symbolTable;

    private static int resultCount;
    public static ResultSymbol generateResultSymbol(){
        return new ResultSymbol("T" + resultCount++);
    }
    public void Compile(String out) throws CompileException, IOException, UnknownSymbolException {
        Compiler.resultCount = 0;
        // Intermediate code generation
        this.generateIntermediate();

        // Output intel assembly
        this.generateAssembly();
    }

    public void generateIntermediate() throws UnknownSymbolException, CompileException {
        this.symbolTable.push(new ArrayList<>());

        for(Stmt stmt : stmts){
            this.intermediate.add(stmt.compile(symbolTable));
        }

        List<Symbol> functions = symbolTable.peek();
        for(int i = 0; i < functions.size(); i++){
            System.out.println(functions.get(i));
            List<Quad> intermediates = intermediate.get(i);
            for(int j = 0; j < intermediates.size(); j++){
                System.out.printf("%d\t%s%n", j, intermediates.get(j));
            }
            System.out.println();
        }

    }

    public void generateAssembly(){

    }

    public Compiler(List<Stmt> stmts){
        this.stmts = stmts;
        this.intermediate = new ArrayList<>();
        this.symbolTable = new Stack<>();
    }
}
