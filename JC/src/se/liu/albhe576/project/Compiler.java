package se.liu.albhe576.project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {

    private final List<Stmt> stmts;

    private final List<List<Quad>> intermediate;
    private final Stack<List<Symbol>> symbolTable;

    private static int resultCount;
    private static int labelCount;
    public static ResultSymbol generateResultSymbol(){
        return new ResultSymbol("T" + resultCount++);
    }
    public static ResultSymbol generateLabel(){
        return new ResultSymbol(String.format("label%d", labelCount++));
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
            Symbol functionSymbol = functions.get(i);
            System.out.println("\n" + functionSymbol.name + ":\n");

            List<Quad> intermediates = intermediate.get(i);
            for(int j = 0; j < intermediates.size(); j++){
                Quad intermediate = intermediates.get(j);
                System.out.printf("%d\t%s%n", j, intermediate);
            }
            System.out.println();
        }

    }

    public void generateAssembly() throws IOException {
        List<Symbol> functions = symbolTable.peek();
        FileWriter fileWriter = new FileWriter("out.asm");
        StringBuilder header = new StringBuilder();
        header.append("global _start\n");
        header.append("section .text\n");
        header.append("_start:\n");
        header.append("push rbp\n");
        header.append("call main\n");
        header.append("pop rbp\n");
        header.append("mov rbx, rax\n");
        header.append("mov rax, 1\n");
        header.append("int 0x80\n");
        fileWriter.write(header.toString());

        List<FunctionSymbol> functionSymbols = new ArrayList<>();
        for(int i = 0; i < functions.size(); i++){
            Symbol symbol = functions.get(i);
            if(symbol instanceof StructSymbol){
                continue;
            }
            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
            functionSymbols.add(functionSymbol);

            fileWriter.write("\n\n" + functionSymbol.name + ":\n");

            List<Quad> intermediates = intermediate.get(i);
            Map<String, Integer> stackVariables = new HashMap<>();
            QuadOp prevOp = null;

            // push the arguments to the stack
            // and store their location
            List<StructField> arguments = functionSymbol.arguments;
            for (StructField arg : arguments) {
                stackVariables.put(arg.name, stackVariables.size());
            }

            for (Quad intermediate : intermediates) {
                fileWriter.write(intermediate.emit(prevOp, stackVariables, functionSymbol) + "\n");
                prevOp = intermediate.op;
            }
        }

        fileWriter.flush();
        fileWriter.close();

    }

    public Compiler(List<Stmt> stmts){
        this.stmts = stmts;
        this.intermediate = new ArrayList<>();
        this.symbolTable = new Stack<>();
    }
}
