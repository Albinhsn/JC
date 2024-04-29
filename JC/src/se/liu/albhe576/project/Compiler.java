package se.liu.albhe576.project;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {

    private final Map<String, Function> functions;
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
    // This is the same as saying (scopeSize == 16 ? 0 : (16 - (scopeSize % 16))
    public static int getStackPadding(int stackSize){return (16 - (stackSize % 16)) & 0xF;}
    public void compile(String name) throws CompileException, IOException{
        Map<String, QuadList> functionQuads = new HashMap<>();

        System.out.println("Compiling!");

        // Intermediate code generation
        for(Map.Entry<String, Function> entry: this.symbolTable.getInternalFunctions().entrySet()){
            Function function                        = entry.getValue();
            Map<String, VariableSymbol> localSymbols = new HashMap<>();
            QuadList quads                           = new QuadList();

            // IP and RBP
            int offset = 16;
            for(StructField arg : function.getArguments()){
                localSymbols.put(arg.name(), new VariableSymbol(arg.name(), arg.type(), offset));
                offset += symbolTable.getStructSize(arg.type());
            }
            symbolTable.compileFunction(entry.getKey(), localSymbols);
            Stmt.compileBlock(symbolTable, quads, function.getBody());
            functionQuads.put(entry.getKey(), quads);
        }
        System.out.println("Compiled!");

        // Output intel assembly
        this.generateAssembly(name, functionQuads);
    }

    private static void outputConstants(StringBuilder footer, Map<String,Constant> constants){
        footer.append("\n\nsection .data\n");
        for(Map.Entry<String, Constant> entry : constants.entrySet()){
            Constant value = entry.getValue();
            if(value.type() == DataTypes.STRING){
                footer.append(String.format("%s db ", value.label()));

                String formatted = entry.getKey().replace("\\n", "\n");
                for (byte b : formatted.getBytes()) {
                    footer.append(String.format("%d, ", b));
                }
                footer.append("0\n");

            }else{
                footer.append(String.format("%s dd %s\n", value.label(), entry.getKey()));
            }
        }
    }

    private static StringBuilder initOutput(Map<String, Function> extern) {
        StringBuilder header = new StringBuilder();
        header.append("global _start\n");

        for(String functionName : extern.keySet()){
            header.append(String.format("extern %s\n", functionName));
        }

        header.append("\n\nsection .text\n");
        header.append("_start:\n");
        header.append("call main\n");
        header.append("mov rbx, rax\n");
        header.append("mov rax, 1\n");
        header.append("int 0x80\n");

        return header;
    }

    private void outputFunctionBody(Optimizer optimizer, StringBuilder stringBuilder, QuadList quads, String functionName) throws CompileException {
        final Map<String, Constant> constants   = this.symbolTable.getConstants();
        final Map<String, Struct> structs       = this.symbolTable.getStructs();

        LinkedList<Instruction> functionInstruction = new LinkedList<>();

        int scopeSize = this.symbolTable.getLocalVariableStackSize(functionName);
        scopeSize += getStackPadding(scopeSize);
        if(scopeSize != 0){
            functionInstruction.add(new Instruction(Operation.SUB, RegisterType.RSP, new Immediate(String.valueOf(scopeSize))));
        }

        for (Quad intermediate : quads) {
            Instruction[] instruction = intermediate.emitInstruction(functions, constants, structs);
            functionInstruction.addAll(Arrays.stream(instruction).toList());
        }

        if(quads.isEmpty() || quads.getLastOp() != QuadOp.RET){
            Quad retQuad = new Quad(QuadOp.RET, null, null, null);
            Instruction[] instruction = retQuad.emitInstruction(functions, constants, structs);
            functionInstruction.addAll(Arrays.stream(instruction).toList());
        }

        optimizer.optimize(functionInstruction);

        for (Instruction instruction : functionInstruction) {
            stringBuilder.append(instruction.emit()).append("\n");
        }

    }

    public String getPrologue(String functionName){return String.format("\n\n%s:\npush rbp\nmov rbp, rsp\n", functionName);}

    public Map<String, Point> removedMap = new HashMap<>();

    public void generateAssembly(String name, Map<String, QuadList> functionQuads) throws IOException, CompileException {
        StringBuilder stringBuilder = initOutput(this.symbolTable.getExternalFunctions());
        Optimizer optimizer         = new Optimizer(removedMap);

        for (String key : this.symbolTable.getInternalFunctions().keySet()) {
            System.out.printf("Optimizing %s\n", key);
            stringBuilder.append(getPrologue(key));
            this.outputFunctionBody(optimizer, stringBuilder, functionQuads.get(key) ,key);
        }

        System.out.println("Optimizing result:");
        OptimizationResults comp = new OptimizationResults();

        int sum = 0;
        int count = 0;
        for(Map.Entry<String, Point> removed : this.removedMap.entrySet().stream().sorted(Collections.reverseOrder(comp)).toList()){
            Point point = removed.getValue();
            System.out.printf("\t%s: %d -> %d\n", removed.getKey(), point.x, point.y);
            sum += point.y;
            count += point.x;
        }
        System.out.printf("Total removed: %d -> %d\n", count, sum);

        Compiler.outputConstants(stringBuilder, this.symbolTable.getConstants());

        FileWriter fileWriter = new FileWriter(name);
        fileWriter.write(stringBuilder.toString());
        fileWriter.flush();
        fileWriter.close();
    }

    public Compiler(Map<String, Struct> structs, Map<String, Function> functions){
        this.functions                  = functions;
        this.symbolTable                = new SymbolTable(structs, functions);
    }
}
