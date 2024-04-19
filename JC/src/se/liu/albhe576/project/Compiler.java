package se.liu.albhe576.project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {

    private final List<Function> extern;
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
        return new Symbol( String.format("label%d", labelCount++), new DataType("label", DataTypes.VOID, 0));
    }

    public void Compile(String name) throws CompileException, IOException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {

        // Intermediate code generation
        this.generateIntermediate();

        // Output intel assembly
        this.generateAssembly(name, extern);
    }


    public void generateIntermediate() throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        for(Stmt stmt : stmts){
            QuadList quads = new QuadList();
            stmt.compile(symbolTable, quads);
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

    private static FileWriter initOutput(String name, Map<String, Constant> constants, List<Function> extern) throws IOException {
        StringBuilder header = new StringBuilder();
        header.append("global _start\n");

        header.append("extern printf\n");
        for(Function function : extern){
            header.append(String.format("extern %s\n", function.name));
        }

        header.append("\n\nsection .data\n");
        for(Map.Entry<String, Constant> entry : constants.entrySet()){
            Constant value = entry.getValue();
            if(value.type == DataTypes.STRING){
                header.append(String.format("%s db \"%s\", 10, 0\n", value.label, entry.getKey()));
            }else{
                header.append(String.format("%s dq %s\n", value.label, entry.getKey()));
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



    public void generateAssembly(String name, List<Function> extern) throws IOException, UnknownSymbolException {
        final List<Function> functions = this.symbolTable.getFunctions();
        final Map<String, Constant> constants = this.symbolTable.getConstants();
        FileWriter fileWriter = initOutput(name, constants, extern);

        for(int i = 0; i < functions.size(); i++){
            Function function = functions.get(i);
            fileWriter.write("\n\n" + function.name + ":\npush rbp\nmov rbp, rsp\n");


            int scopeSize = this.symbolTable.getScopeSize(function.name);
            scopeSize += (this.symbolTable.getFunctionSize(function.name) % 16) == 0 ? 0 : 8;
            if(scopeSize != 0){
                fileWriter.write(String.format("sub rsp, %d\n", scopeSize));
            }

            Stack stack = new Stack(this.symbolTable.getLocals(function.name), this.symbolTable.structs);
            for (Quad intermediate : function.intermediates) {
                // fileWriter.write("; " + intermediate + "\n");
                fileWriter.write(intermediate.emit(stack, functions, constants) + "\n");
            }
            Quad lastQuad = function.intermediates.get(function.intermediates.size() - 1);
            if(lastQuad.op != QuadOp.RET){
                Quad retQuad = new Quad(QuadOp.RET, null, null, null);
                fileWriter.write(retQuad.emit(stack, functions, constants) + "\n");
            }
        }

        fileWriter.flush();
        fileWriter.close();

    }

    public Compiler(Map<String, Struct> structs, List<Stmt> stmts, List<Function> extern){
        this.stmts = stmts;
        Map<String, Constant> constants = new HashMap<>();
        this.extern = extern;
        this.symbolTable = new SymbolTable(structs, constants, extern);
    }
}
