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

        // Intermediate code generation
        this.generateIntermediate();

        // Output intel assembly
        this.generateAssembly(name);
    }


    public void generateIntermediate() throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        QuadList quads = new QuadList();
        for(Stmt stmt : stmts){
            quads.concat(stmt.compile(symbolTable));
        }

        List<Function> functions = symbolTable.getFunctions();
        for(Function function : functions){
            System.out.println("\n" + function.name + ":\n");


            for(int i = 0; i < function.intermediates.size(); i++){
                Quad intermediate = function.intermediates.get(i);
                System.out.printf("%d\t%s%n", i, intermediate);
            }
            System.out.println();
        }

    }

    private static FileWriter initOutput(String name, Map<String, Constant> constants) throws IOException {
        StringBuilder header = new StringBuilder();
        header.append("global _start\n");

        header.append("extern malloc\n");
        header.append("extern free\n");
        header.append("extern printf\n");

        header.append("\n\nsection .data\n");
        for(Map.Entry<String, Constant> entry : constants.entrySet()){
            Constant value = entry.getValue();
            if(value.type == DataTypes.BYTE_POINTER){
                header.append(String.format("%s db \"%s\", 10, 0\n", value.label, entry.getKey()));
            }else{
                header.append(String.format("%s dd %s\n", entry.getValue(), entry.getKey()));
            }
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
        final List<Function> functions = this.symbolTable.getFunctions();
        final Map<String, Constant> constants = this.symbolTable.getConstants();
        FileWriter fileWriter = initOutput(name, constants);

        for(Function function : functions){
            fileWriter.write("\n\n" + function.name + ":\npush rbp\nmov rbp, rsp\n");

            Quad prev = null;
            Stack stack = new Stack(function.arguments, this.symbolTable.structs);

            for (Quad intermediate : function.intermediates.getQuads()) {
                //fileWriter.write("; " + intermediate + "\n");
                fileWriter.write(intermediate.emit(stack, prev, functions, constants) + "\n");
                prev = intermediate;
            }
            Quad lastQuad = function.intermediates.get(function.intermediates.size() - 1);
            if(lastQuad.op != QuadOp.RET){
                Quad retQuad = new Quad(QuadOp.RET, null, null, null);
                fileWriter.write(retQuad.emit(stack, lastQuad, functions, constants) + "\n");
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
