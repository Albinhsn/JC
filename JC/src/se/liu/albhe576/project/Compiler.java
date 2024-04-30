package se.liu.albhe576.project;

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
    /*
     *  This is the same as saying (scopeSize == 16 ? 0 : (16 - (scopeSize % 16))
     */
    public static int getStackPadding(int stackSize){return (16 - (stackSize % 16)) & 0xF;}
    public void compile(String name) throws CompileException, IOException{
        final Map<String, QuadList> functionQuads = new HashMap<>();

        // Intermediate code generation
        for(Map.Entry<String, Function> functionEntry: this.symbolTable.getInternalFunctions().entrySet()){
            final Function function                        = functionEntry.getValue();
            final Map<String, VariableSymbol> localSymbols = new HashMap<>();
            final QuadList quads                           = new QuadList();

            // IP and RBP
            int offset = 16;
            for(StructField arg : function.getArguments()){
                localSymbols.put(arg.name(), new VariableSymbol(arg.name(), arg.type(), offset));
                offset += symbolTable.getStructSize(arg.type());
            }

            symbolTable.compileFunction(functionEntry.getKey(), localSymbols);
            Stmt.compileBlock(symbolTable, quads, function.getBody());
            functionQuads.put(functionEntry.getKey(), quads);
        }

        // Output intel assembly
        this.generateAssembly(name, functionQuads);
    }

    private static void outputConstantData(StringBuilder constantData, Map<String,Constant> constants){
        constantData.append("\n\nsection .data\n");
        for(Map.Entry<String, Constant> entry : constants.entrySet()){
            Constant value = entry.getValue();
            if(value.type() == DataTypes.STRING){
                constantData.append(String.format("%s db ", value.label()));

                String formatted = entry.getKey().replace("\\n", "\n");
                for (byte b : formatted.getBytes()) {
                    constantData.append(String.format("%d, ", b));
                }
                constantData.append("0\n");
            }else{
                constantData.append(String.format("%s dd %s\n", value.label(), entry.getKey()));
            }
        }
    }

    private static void initOutput(StringBuilder header, Map<String, Function> extern) {
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
    }

    private List<Instruction> outputFunctionBody(QuadList quads, String functionName) throws CompileException {
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
            Quad retQuad                = new Quad(QuadOp.RET, null, null, null);
            Instruction[] instruction   = retQuad.emitInstruction(functions, constants, structs);
            functionInstruction.addAll(Arrays.stream(instruction).toList());
        }

        return functionInstruction;
    }

    public String getFunctionPrologue(String functionName){return String.format("\n\n%s:\npush rbp\nmov rbp, rsp\n", functionName);}

    public void generateAssembly(String name, Map<String, QuadList> functionQuads) throws IOException, CompileException {
        StringBuilder stringBuilder = new StringBuilder();
        outputConstantData(stringBuilder, this.symbolTable.getConstants());
        initOutput(stringBuilder, this.symbolTable.getExternalFunctions());

        for (String functionName : this.symbolTable.getInternalFunctions().keySet()) {
            stringBuilder.append(this.getFunctionPrologue(functionName));
            for(Instruction instruction : this.outputFunctionBody(functionQuads.get(functionName), functionName)){
                stringBuilder.append(instruction.emit()).append("\n");
            }
        }
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
