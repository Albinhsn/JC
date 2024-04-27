package se.liu.albhe576.project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {

    private final Map<String, Function> functions;
    private final Map<String, QuadList> functionQuads;
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
        for(Map.Entry<String, Function> entry: this.functions.entrySet().stream().filter(x -> !x.getValue().external).toList()){
            Function function = entry.getValue();
            Map<String, VariableSymbol> localSymbols = new HashMap<>();

            QuadList quads = new QuadList();
            this.functionQuads.put(entry.getKey(), quads);

            int offset = 16;
            for(StructField arg : function.getArguments()){
                localSymbols.put(arg.name(), new VariableSymbol(arg.name(), arg.type(), offset, symbolTable.generateVariableId()));
                offset += SymbolTable.getStructSize(symbolTable.getStructs(), arg.type());
            }
            symbolTable.compileFunction(entry.getKey(), localSymbols);
            Stmt.compileBlock(symbolTable, quads, function.getBody());
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
    private void outputFunctionBody(StringBuilder stringBuilder, QuadList quads) throws CompileException {
        final Map<String, Constant> constants = this.symbolTable.getConstants();
        final Map<String, Struct> structs = this.symbolTable.getStructs();


        for (Quad intermediate : quads) {
            stringBuilder.append(String.format("; %s\n", intermediate));
            System.out.printf("; %s\n", intermediate);
            Instruction[] instruction = intermediate.emitInstruction(functions, constants, structs);
            for(Instruction ins : instruction){
                stringBuilder.append(ins.emit().toLowerCase()).append("\n");
            }
        }

        if(quads.isEmpty() || quads.getLastOp() != QuadOp.RET){
            Quad retQuad = new Quad(QuadOp.RET, null, null, null);
            Instruction[] instruction = retQuad.emitInstruction(functions, constants, structs);
            for(Instruction ins : instruction){
                stringBuilder.append(ins.emit().toLowerCase()).append("\n");
            }
        }

    }

    public void generateAssembly(String name) throws IOException, CompileException {
        final Map<String, Function> functions = this.symbolTable.getInternalFunctions();
        StringBuilder stringBuilder = initOutput(this.symbolTable.getConstants(), this.symbolTable.getExternalFunctions());

        for (String key : functions.keySet()) {
            stringBuilder.append(String.format("\n\n%s:\npush rbp\nmov rbp, rsp\n", key));
            this.handleStackAlignment(stringBuilder, key);
            this.outputFunctionBody(stringBuilder, this.functionQuads.get(key));
        }

        FileWriter fileWriter = new FileWriter(name);
        fileWriter.write(stringBuilder.toString());
        fileWriter.flush();
        fileWriter.close();
    }

    public Compiler(Map<String, Struct> structs, Map<String, Function> functions){
        this.functions = functions;
        this.functionQuads = new HashMap<>();
        Map<String, Constant> constants = new HashMap<>();
        this.symbolTable = new SymbolTable(structs, constants, functions);
    }
}
