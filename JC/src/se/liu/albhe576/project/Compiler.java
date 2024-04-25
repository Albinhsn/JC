package se.liu.albhe576.project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {

    private final List<Stmt> stmts;
    private final SymbolTable symbolTable;
    private static int resultCount;
    private static int labelCount;
    public static void error(String msg, int line, String filename) throws CompileException{
        System.out.printf("%s:%d[%s]", filename,line,msg);
        System.exit(1);
    }

    public static Symbol generateSymbol(DataType type){
        return new Symbol("T" + resultCount++, type);
    }
    public static ImmediateSymbol generateImmediateSymbol(DataType type, String literal){return new ImmediateSymbol("T" + resultCount++, type, literal);}
    public static Symbol generateLabel(){return new Symbol( String.format("label%d", labelCount++), new DataType("label", DataTypes.VOID, 0));}
    public static int getStackPadding(int stackSize){
        // This is the same as saying (scopeSize == 16 ? 0 : (16 - (scopeSize % 16))
        return (16 - (stackSize % 16)) & 0xF;
    }

    public void compile(String name) throws CompileException, IOException{

        // Intermediate code generation
        for(Stmt stmt : stmts){
            QuadList quads = new QuadList();
            stmt.compile(symbolTable, quads);
        }

        // Output intel assembly
        this.generateAssembly(name);
    }

    private static StringBuilder initOutput(Map<String, Constant> constants, Map<String, Function> extern) {
        StringBuilder header = new StringBuilder();
        header.append("global _start\n");

        for(String functionName : extern.keySet()){
            header.append(String.format("extern %s\n", functionName));
        }

        header.append("\n\nsection .data\n");
        for(Map.Entry<String, Constant> entry : constants.entrySet()){
            Constant value = entry.getValue();
            if(value.type() == DataTypes.STRING){
                header.append(String.format("%s db ", value.label()));

                String formatted = entry.getKey().replace("\\n", "\n");
                for (byte b : formatted.getBytes()) {
                    header.append(String.format("%d, ", b));
                }
                header.append("0\n");

            }else{
                header.append(String.format("%s dd %s\n", value.label(), entry.getKey()));
            }
        }
        header.append("\n\nsection .text\n");
        header.append("_start:\n");
        header.append("call main\n");
        header.append("mov rbx, rax\n");
        header.append("mov rax, 1\n");
        header.append("int 0x80\n");

        return header;
    }

    private void handleStackAlignment(StringBuilder stringBuilder,  String functionName) {
        int scopeSize = this.symbolTable.getLocalVariableStackSize(functionName);
        scopeSize += getStackPadding(scopeSize);
        if(scopeSize != 0){
            stringBuilder.append(String.format("sub rsp, %d\n", scopeSize));
        }
    }
    private void outputFunctionBody(StringBuilder stringBuilder, String functionName, Function function) throws CompileException {
        final Map<String, Function> functions = this.symbolTable.getAllFunctions();
        final Map<String, Constant> constants = this.symbolTable.getConstants();

        Stack stack = new Stack(this.symbolTable.getAllScopedVariables(functionName), this.symbolTable.getStructs());

        QuadList intermediates = function.getIntermediates();
        for (Quad intermediate : intermediates) {
            stringBuilder.append(String.format("; %s\n", intermediate));
            stringBuilder.append(intermediate.emit(stack, functions, constants)).append("\n");
        }

        if(intermediates.isEmpty() || intermediates.getLastOp() != QuadOp.RET){
            Quad retQuad = new Quad(QuadOp.RET, null, null, null);
            stringBuilder.append(retQuad.emit(stack, functions, constants)).append("\n");
        }

    }

    public void generateAssembly(String name) throws IOException, CompileException {
        final Map<String, Function> functions = this.symbolTable.getInternalFunctions();
        StringBuilder stringBuilder = initOutput(this.symbolTable.getConstants(), this.symbolTable.getExternalFunctions());

        for (Map.Entry<String, Function> function : functions.entrySet()) {
            String key = function.getKey();
            stringBuilder.append(String.format("\n\n%s:\npush rbp\nmov rbp, rsp\n", key));
            this.handleStackAlignment(stringBuilder, key);
            this.outputFunctionBody(stringBuilder, key, function.getValue());
        }

        FileWriter fileWriter = new FileWriter(name);
        fileWriter.write(stringBuilder.toString());
        fileWriter.flush();
        fileWriter.close();
    }

    public Compiler(Map<String, Struct> structs, List<Stmt> stmts, Map<String, Function> extern){
        this.stmts = stmts;
        Map<String, Constant> constants = new HashMap<>();
        this.symbolTable = new SymbolTable(structs, constants, extern);
    }
}
