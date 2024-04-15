package se.liu.albhe576.project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {

    private final List<Stmt> stmts;
    private final List<StructSymbol> structSymbols;

    private final List<List<Quad>> intermediate;
    private final java.util.Stack<List<Symbol>> symbolTable;

    private static int resultCount;
    private static int labelCount;
    public static ResultSymbol generateResultSymbol(){
        return new ResultSymbol("T" + resultCount++);
    }
    public static ResultSymbol generateLabel(){
        return new ResultSymbol(String.format("label%d", labelCount++));
    }
    public void Compile(String name) throws CompileException, IOException, UnknownSymbolException {
        Compiler.resultCount = 0;
        Compiler.labelCount = 0;
        // Intermediate code generation
        this.generateIntermediate();

        // Output intel assembly
        this.generateAssembly(name);
    }


    public void generateIntermediate() throws UnknownSymbolException, CompileException {
        this.symbolTable.push(new ArrayList<>());

        for(Stmt stmt : stmts){
            List<Quad> inter = stmt.compile(this.structSymbols, symbolTable);
            if(!inter.isEmpty()){
                this.intermediate.add(inter);
            }
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

    private static FileWriter initOutput(String name) throws IOException {
        StringBuilder header = new StringBuilder();
        header.append("global _start\n");
        header.append("section .text\n");
        header.append("_start:\n");
        header.append("call main\n");
        header.append("mov rbx, rax\n");
        header.append("mov rax, 1\n");
        header.append("int 0x80\n");

        FileWriter writer = new FileWriter(name);
        writer.write(header.toString());
        return writer;
    }

    public static StructSymbol lookupStruct(List<StructSymbol> structSymbols, String name) throws UnknownSymbolException {
        System.out.printf("Looking for %s\n", name);
        for(StructSymbol symbol : structSymbols){
            System.out.printf("Checking vs %s\n", symbol.name);
            if(symbol.name.equals(name)){
                return symbol;
            }
        }
        throw new UnknownSymbolException(String.format("Couldn't find struct '%s'", name));
    }

    private Stack createStack(List<StructField> arguments) throws UnknownSymbolException {
        List<StructSymbol> args = new ArrayList<>();
        List<String> argNames = new ArrayList<>();
        for (StructField arg : arguments) {
            args.add(lookupStruct(structSymbols, arg.structName));

        }
        return new Stack(args, argNames);
    }

    public void generateAssembly(String name) throws IOException, UnknownSymbolException {
        List<Symbol> functions = symbolTable.peek();
        FileWriter fileWriter = initOutput(name);

        List<FunctionSymbol> functionSymbols = new ArrayList<>();
        for(int i = 0; i < functions.size(); i++){
            Symbol symbol = functions.get(i);
            if(symbol instanceof StructSymbol){
                this.structSymbols.add((StructSymbol) symbol);
                continue;
            }

            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
            functionSymbols.add(functionSymbol);

            fileWriter.write("\n\n" + functionSymbol.name + ":\npush rbp\nmov rbp, rsp\n");


            List<Quad> intermediates = intermediate.get(i);
            QuadOp prevOp = null;

            Stack stack = this.createStack(functionSymbol.arguments);

            for (Quad intermediate : intermediates) {
                fileWriter.write(intermediate.emit(stack, prevOp, functionSymbols, this.structSymbols) + "\n");
                prevOp = intermediate.op;
            }
        }

        fileWriter.flush();
        fileWriter.close();

    }

    public Compiler(List<Stmt> stmts){
        this.stmts = stmts;
        this.intermediate = new ArrayList<>();
        this.symbolTable = new java.util.Stack<>();
        this.structSymbols = new ArrayList<>();
        StructSymbol[] symbols = new StructSymbol[]{
            new StructSymbol("int", new ArrayList<>(List.of(new StructField[]{
                    new StructField("int", StructType.INT, "int")
            })))
        };
        this.structSymbols.addAll(List.of(symbols));
    }
}
