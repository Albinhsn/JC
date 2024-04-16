package se.liu.albhe576.project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {

    private final List<Stmt> stmts;
    private final SymbolTable symbolTable;
    private static int resultCount;
    private static int labelCount;
    public static Symbol generateSymbol(DataType type){
        return new Symbol("T" + resultCount++, type);
    }
    public static ImmediateSymbol generateImmediateSymbol(DataType type, String literal){
        return new ImmediateSymbol("T" + resultCount++, type, literal);
    }
    public static Symbol generateLabel(){
        return new Symbol( String.format("label%d", labelCount++), new DataType("label", DataTypes.VOID));
    }
    public void Compile(String name) throws CompileException, IOException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {
        Compiler.resultCount = 0;
        Compiler.labelCount = 0;
        // Intermediate code generation
        this.generateIntermediate();

        // Output intel assembly
        this.generateAssembly(name);
    }


    public void generateIntermediate() throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {

        for(Stmt stmt : stmts){
            stmt.compile(symbolTable);
        }

        List<Function> functions = symbolTable.functions;
        for(Function function : functions){
            System.out.println("\n" + function.name + ":\n");

            for(int i = 0; i < function.intermediates.size(); i++){
                Quad intermediate = function.intermediates.get(i);
                System.out.printf("%d\t%s%n", i, intermediate);
            }
            System.out.println();
        }

    }

    private static FileWriter initOutput(String name, Map<String, String> constants) throws IOException {
        StringBuilder header = new StringBuilder();
        header.append("global _start\n");

        header.append("\n\nsection .data\n");
        for(Map.Entry<String, String> entry : constants.entrySet()){
            header.append(String.format("%s dd %s\n", entry.getValue(), entry.getKey()));
        }
        header.append("\n\nsection .text\n");
        header.append("_start:\n");
        header.append("call main\n");
        header.append("mov rbx, rax\n");
        header.append("mov rax, 1\n");
        header.append("int 0x80\n");

        FileWriter writer = new FileWriter(name);
        writer.write(header.toString());
        return writer;
    }



    public void generateAssembly(String name) throws IOException, UnknownSymbolException {
        FileWriter fileWriter = initOutput(name, this.symbolTable.constants);

        for(Function function : this.symbolTable.functions){
            fileWriter.write("\n\n" + function.name + ":\npush rbp\nmov rbp, rsp\n");

            QuadOp prevOp = null;
            Stack stack = new Stack(function.arguments, this.symbolTable.structs);

            for (Quad intermediate : function.intermediates) {
                fileWriter.write(intermediate.emit(stack, prevOp, this.symbolTable.functions, this.symbolTable.constants) + "\n");
                prevOp = intermediate.op;
            }
        }

        fileWriter.flush();
        fileWriter.close();

    }

    public Compiler(List<Stmt> stmts){
        this.stmts = stmts;
        this.symbolTable = new SymbolTable();
    }
}
