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

    public void Compile(String name) throws CompileException, IOException {

        // Intermediate code generation
        this.generateIntermediate();

        // Output intel assembly
        this.generateAssembly(name, extern);
    }

    private void debugIntermediates(){

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

    public void generateIntermediate() throws CompileException{
        for(Stmt stmt : stmts){
            QuadList quads = new QuadList();
            stmt.compile(symbolTable, quads);
        }

        this.debugIntermediates();


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
            String key = entry.getKey();
            if(value.type == DataTypes.STRING){
                header.append(String.format("%s db ", value.label));
                if(key.isEmpty()){
                    header.append(" 0");
                }else{
                    String formatted = key.replace("\\n", "\n");
                    header.append(String.format("%d",(byte)formatted.charAt(0)));
                    byte[] keyAsBytes = formatted.getBytes();
                    for(int i = 1; i < keyAsBytes.length; i++){
                        byte b = keyAsBytes[i];
                        header.append(String.format(", %d",b));
                    }
                    header.append(", 0\n");
                }

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

    private void handleStackAlignment(FileWriter fileWriter, Function function) throws IOException, CompileException {
        int scopeSize = this.symbolTable.getLocalVariableStackSize(function.name);
        scopeSize += (scopeSize % 16) == 0 ? 0 : 8;
        if(scopeSize != 0){
            fileWriter.write(String.format("sub rsp, %d\n", scopeSize));
        }
    }
    private void outputFunctionHeader(FileWriter fileWriter, Function function) throws IOException {
        fileWriter.write("\n\n" + function.name + ":\npush rbp\nmov rbp, rsp\n");
    }

    private void outputFunctionBody(FileWriter fileWriter, Function function) throws IOException, CompileException {
        final List<Function> functions = this.symbolTable.getFunctions();
        final Map<String, Constant> constants = this.symbolTable.getConstants();

        Stack stack = new Stack(this.symbolTable.getLocals(function.name), this.symbolTable.structs);

        boolean shouldOutputRet = function.intermediates.isEmpty();
        for (Quad intermediate : function.intermediates) {
            // fileWriter.write("; " + intermediate + "\n");
            fileWriter.write(intermediate.emit(stack, functions, constants) + "\n");
        }

        if(!shouldOutputRet){
            Quad lastQuad = function.intermediates.get(function.intermediates.size() - 1);
            shouldOutputRet = lastQuad.op != QuadOp.RET;
        }

        if(shouldOutputRet){
            Quad retQuad = new Quad(QuadOp.RET, null, null, null);
            fileWriter.write(retQuad.emit(stack, functions, constants) + "\n");
        }

    }

    public void generateAssembly(String name, List<Function> extern) throws IOException, CompileException {
        final List<Function> functions = this.symbolTable.getFunctions();
        final Map<String, Constant> constants = this.symbolTable.getConstants();
        FileWriter fileWriter = initOutput(name, constants, extern);

        for (Function function : functions) {
            this.outputFunctionHeader(fileWriter, function);
            this.handleStackAlignment(fileWriter, function);
            this.outputFunctionBody(fileWriter, function);
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
