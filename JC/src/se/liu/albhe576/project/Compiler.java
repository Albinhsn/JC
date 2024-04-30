package se.liu.albhe576.project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {
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
    public Map<String, QuadList> generateIntermediate() throws CompileException {
        final Map<String, QuadList> functionIntermediates = new HashMap<>();
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
            System.out.printf("\n%s\n", functionEntry.getKey());
            for(Quad quad : quads){
                System.out.println(quad);
            }
            functionIntermediates.put(functionEntry.getKey(), quads);
        }
        return functionIntermediates;
    }
    private static final Instruction[] PROLOGUE_INSTRUCTIONS = new Instruction[]{
            new Instruction(Operation.PUSH, new Address(Register.RBP), null),
            new Instruction(Operation.MOV, new Address(Register.RBP), new Address(Register.RSP))
    };
    private static final Instruction[] EPILOGUE_INSTRUCTIONS = new Instruction[]{
            new Instruction(Operation.MOV, new Address(Register.RSP), new Address(Register.RBP)),
            new Instruction(Operation.POP, new Address(Register.RBP), null)
    };
    public static int getStackAlignment(int stackSize){
        final int alignmentInBytes = 16;
        // is essentially
        //   if stackSize % alignmentInBytes == 0 ? 0 : (alignmentInBytes - stackSize) % alignmentInBytes
        // just done to avoid a branch :)
        // works since alignment is a power of 2
        return (alignmentInBytes - (stackSize % alignmentInBytes)) & (alignmentInBytes - 1);
    }
    private Map<String, List<Instruction>> generateAssembly(Map<String, Constant> constants, Map<String, QuadList> functionQuads) throws CompileException {
        Map<String, List<Instruction>> generatedAssembly = new HashMap<>();


        for(Map.Entry<String, QuadList> functions : functionQuads.entrySet()){
            final String name                   = functions.getKey();
            final QuadList quads                = functions.getValue();
            int stackSize                       = this.symbolTable.getLocalVariableStackSize(name);
            TemporaryVariableStack tempStack    = new TemporaryVariableStack(stackSize);

            // add prologue
            List<Instruction> instructions = new LinkedList<>(List.of(PROLOGUE_INSTRUCTIONS));
            for(Quad quad : quads){
                instructions.addAll(quad.emitInstructions(symbolTable, constants, tempStack));
            }
            // Check if we need to insert a return value or not


            // calculate stack size and padding
            final int stackAllocationInstructionIndex = PROLOGUE_INSTRUCTIONS.length;
            final int stackAlignment                  = -tempStack.maxOffset + getStackAlignment(tempStack.maxOffset);
            instructions.add(stackAllocationInstructionIndex, new Instruction(Operation.SUB, new Address(Register.RSP), new Immediate(stackAlignment)));

            generatedAssembly.put(name, instructions);
        }
        return generatedAssembly;
    }
    public void compile(String fileName) throws CompileException, IOException{
        Optimizer optimizer = new Optimizer();

        // Intermediate code generation
        final Map<String, QuadList> functionQuads = this.generateIntermediate();
        optimizer.optimizeIntermediates();

        // generate assembly
        Map<String, List<Instruction>> instructions = this.generateAssembly(this.symbolTable.getConstants(), functionQuads);
        optimizer.optimizeX86Assembly();

        // output assembly
        this.outputX86Assembly(fileName, instructions);
    }

    public void outputX86Assembly(String fileName, Map<String, List<Instruction>> functions) throws IOException {

        FileWriter fileWriter = new FileWriter(fileName);

        // To append -> one write?
        fileWriter.write("global _start\n\nsection .data\n");

        for(Map.Entry<String, Constant> entry : this.symbolTable.getConstants().entrySet()){
            Constant value = entry.getValue();
            if(value.type() == DataTypes.STRING){
                fileWriter.write(String.format("%s db ", value.label()));

                String formatted = entry.getKey().replace("\\n", "\n");
                for (byte b : formatted.getBytes()) {
                    fileWriter.write(String.format("%d, ", b));
                }
                fileWriter.write("0\n");
            }else{
                fileWriter.write(String.format("%s dd %s\n", value.label(), entry.getKey()));
            }
        }

        fileWriter.write("\n\nsection .text\n_start:\ncall main\nmov rbx, rax\nmov rax, 1\nint 0x80\n\n");

        for(Map.Entry<String, List<Instruction>> function : functions.entrySet()){
            String functionName = function.getKey();
            fileWriter.write(functionName + ":\n");
            for(Instruction instruction : function.getValue()){
                fileWriter.write(instruction + "\n");
            }

        }
        fileWriter.flush();
        fileWriter.close();
        // output static data
        // output token main function

        // output functions

        // write to file
    }


    // ToDo Hoist out
    public Compiler(Map<String, Struct> structs, Map<String, Function> functions){
        this.symbolTable                = new SymbolTable(structs, functions);
    }
}
