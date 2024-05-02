package se.liu.albhe576.project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {
    private final SymbolTable symbolTable;
    public static void error(String msg, int line, String filename) throws CompileException{
        System.out.printf("%s:%d[%s]", filename,line,msg);
        System.exit(1);
    }
    public Map<String, QuadList> generateIntermediate() throws CompileException {
        final Map<String, QuadList> functionIntermediates = new HashMap<>();
        for(Map.Entry<String, Function> functionEntry: this.symbolTable.getInternalFunctions().entrySet()){
            final Function function                        = functionEntry.getValue();
            final Map<String, VariableSymbol> localSymbols = new HashMap<>();
            final QuadList quads                           = new QuadList();

            // IP and RBP
            int offset = 16;
            System.out.println(functionEntry.getKey());
            for(StructField arg : function.getArguments()){
                localSymbols.put(arg.name(), new VariableSymbol(arg.name(), arg.type(), offset));
                offset += symbolTable.getStructSize(arg.type());
            }

            symbolTable.compileFunction(functionEntry.getKey(), localSymbols);
            Stmt.compileBlock(symbolTable, quads, function.getBody());
            functionIntermediates.put(functionEntry.getKey(), quads);
        }
        return functionIntermediates;
    }
    public static int getStackAlignment(int stackSize){
        final int alignmentInBytes = 16;
        return (alignmentInBytes - (stackSize % alignmentInBytes)) & (alignmentInBytes - 1);
    }
    private void addReturn(InstructionList instructions){
        Instruction lastInstruction = instructions.getLast();
        if(lastInstruction.op == null || lastInstruction.op.getOp() != OperationType.RET){
            instructions.addEpilogue();
            instructions.addReturn();
        }
    }
    private Map<String, InstructionList> generateAssembly(Map<String, Constant> constants, Map<String, QuadList> functionQuads) throws CompileException {
        Map<String, InstructionList> generatedAssembly = new HashMap<>();


        for(Map.Entry<String, QuadList> functions : functionQuads.entrySet()){
            final String name                   = functions.getKey();
            final QuadList quads                = functions.getValue();
            int stackSize                       = this.symbolTable.getLocalVariableStackSize(name);
            TemporaryVariableStack tempStack    = new TemporaryVariableStack(stackSize);

            // add prologue
            InstructionList instructions = new InstructionList();
            instructions.addPrologue();
            final int stackAllocationInstructionIndex = instructions.size();
            System.out.printf("\n\n%s\n", name);
            for(Quad quad : quads){
                System.out.println(quad);
                instructions.addAll(quad.emitInstructions(symbolTable, constants, tempStack));
            }
            // Check if we need to insert a return value or not
            this.addReturn(instructions);

            // calculate stack size and padding
            final int maxOffset                         = -tempStack.maxOffset;
            final int stackAlignment                    = maxOffset + getStackAlignment(maxOffset);
            instructions.allocateStackSpace(stackAlignment, stackAllocationInstructionIndex);

            generatedAssembly.put(name, instructions);
        }
        return generatedAssembly;
    }
    public void compile(String fileName) throws CompileException, IOException{
        // Intermediate code generation
        final Map<String, QuadList> functionQuads = this.generateIntermediate();

        // generate assembly
        Map<String, InstructionList> instructions = this.generateAssembly(this.symbolTable.getConstants(), functionQuads);

        // output assembly
        this.outputX86Assembly(fileName, instructions);
    }

    public String addConstants(){
        StringBuilder stringBuilder = new StringBuilder();
        for(Map.Entry<String, Constant> entry : this.symbolTable.getConstants().entrySet()){
            Constant value = entry.getValue();
            if(value.type() == DataTypes.STRING){
                // Need to replace \n with 10 which is ASCII for new line
                // Should be done for other escape characters as well
                String formatted = entry.getKey().replace("\\n", "\", 10, \"");
                stringBuilder.append(String.format("%s db \"%s\", 0\n", value.label(), formatted));
            }else{
                stringBuilder.append(String.format("%s dd %s\n", value.label(), entry.getKey()));
            }
        }
        return stringBuilder.toString();
    }

    public void outputX86Assembly(String fileName, Map<String, InstructionList> functions) throws IOException {

        FileWriter fileWriter = new FileWriter(fileName);
        for(String externalFunction : symbolTable.getExternalFunctions().keySet()){
            fileWriter.write(String.format("extern %s\n", externalFunction));
        }
        fileWriter.write("global _start\n\nsection .data\n");
        fileWriter.write(this.addConstants());
        fileWriter.write("\n\nsection .text\n_start:\ncall main\nmov rbx, rax\nmov rax, 1\nint 0x80\n\n");

        for(Map.Entry<String, InstructionList> function : functions.entrySet()){
            String functionName = function.getKey();
            fileWriter.write("\n\n" + functionName + ":\n");
            for(Instruction instruction : function.getValue()){
                fileWriter.write(instruction + "\n");
            }

        }
        fileWriter.flush();
        fileWriter.close();
    }

    public Compiler(Map<String, Struct> structs, Map<String, Function> functions){
        this.symbolTable                = new SymbolTable(structs, functions);
    }
}
