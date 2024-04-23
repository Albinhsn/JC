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
        return new Symbol( String.format("label%d", labelCount++), new DataType("label", DataTypes.VOID, 0));
    }

    public void Compile(String name) throws CompileException, IOException {

        // Intermediate code generation
        for(Stmt stmt : stmts){
            QuadList quads = new QuadList();
            stmt.compile(symbolTable, quads);
        }

        final Map<String, Function> functions = this.symbolTable.getInternalFunctions();
        for(Map.Entry<String, Function> function : functions.entrySet()){
            String k = function.getKey();
            Function f = function.getValue();
            System.out.println(k + ":");
            for(Quad quad : f.getIntermediates()){
                System.out.println(quad);
            }
        }

        // Output intel assembly
        this.generateAssembly(name);
    }

    private static FileWriter initOutput(String name, Map<String, Constant> constants, Map<String, Function> extern) throws IOException {
        StringBuilder header = new StringBuilder();
        header.append("global _start\n");

        header.append("extern printf\n");
        for(String functionName : extern.keySet()){
            header.append(String.format("extern %s\n", functionName));
        }

        header.append("\n\nsection .data\n");
        for(Map.Entry<String, Constant> entry : constants.entrySet()){
            Constant value = entry.getValue();
            String key = entry.getKey();
            if(value.type() == DataTypes.STRING){
                header.append(String.format("%s db ", value.label()));
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
                header.append(String.format("%s dq %s\n", value.label(), entry.getKey()));
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

    private void handleStackAlignment(FileWriter fileWriter, String functionName) throws IOException{
        int scopeSize = this.symbolTable.getLocalVariableStackSize(functionName);
        scopeSize += (scopeSize % 16) == 0 ? 0 : (16 - (scopeSize % 16));
        if(scopeSize != 0){
            fileWriter.write(String.format("sub rsp, %d\n", scopeSize));
        }
    }
    private void outputFunctionHeader(FileWriter fileWriter, String functionName) throws IOException {
        fileWriter.write("\n\n" + functionName + ":\npush rbp\nmov rbp, rsp\n");
    }

    private void outputFunctionBody(FileWriter fileWriter, String functionName, Function function) throws IOException, CompileException {
        final Map<String, Function> functions = this.symbolTable.getFunctions();
        final Map<String, Constant> constants = this.symbolTable.getConstants();

        Stack stack = new Stack(this.symbolTable.getAllScopedVariables(functionName), this.symbolTable.getStructs());

        QuadList intermediates = function.getIntermediates();
        boolean shouldOutputRet = intermediates.isEmpty();
        for (Quad intermediate : intermediates) {
            //fileWriter.write("; " + intermediate + "\n");
            fileWriter.write(intermediate.emit(stack, functions, constants) + "\n");
        }

        if(!shouldOutputRet){
            Quad lastQuad = intermediates.getLastQuad();
            shouldOutputRet = lastQuad.getOp() != QuadOp.RET;
        }

        if(shouldOutputRet){
            Quad retQuad = new Quad(QuadOp.RET, null, null, null);
            fileWriter.write(retQuad.emit(stack, functions, constants) + "\n");
        }

    }

    public void generateAssembly(String name) throws IOException, CompileException {
        final Map<String, Function> functions = this.symbolTable.getInternalFunctions();
        final Map<String, Function> extern    = this.symbolTable.getExternalFunctions();
        final Map<String, Constant> constants = this.symbolTable.getConstants();
        FileWriter fileWriter = initOutput(name, constants, extern);

        for (Map.Entry<String, Function> function : functions.entrySet()) {
            String key = function.getKey();
            this.outputFunctionHeader(fileWriter, key);
            this.handleStackAlignment(fileWriter, key);
            this.outputFunctionBody(fileWriter, key, function.getValue());
        }

        fileWriter.flush();
        fileWriter.close();

    }

    public Compiler(Map<String, Struct> structs, List<Stmt> stmts, Map<String, Function> extern){
        this.stmts = stmts;
        Map<String, Constant> constants = new HashMap<>();
        this.symbolTable = new SymbolTable(structs, constants, extern);
    }
}
