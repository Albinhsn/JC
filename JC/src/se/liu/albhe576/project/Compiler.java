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
    private static int getStackAlignment(int stackSize){
        final int alignmentInBytes = 16;
        // is essentially
        //   if stackSize % alignmentInBytes == 0 ? 0 : (alignmentInBytes - stackSize) % alignmentInBytes
        // just done to avoid a branch :)
        // works since alignment is a power of 2
        return (alignmentInBytes - (stackSize % alignmentInBytes)) & (alignmentInBytes - 1);
    }
    private int allocateStackSpace(List<Integer> cache, String name) throws CompileException {
        Function function = this.symbolTable.getFunction(name);
        int stackSize = this.symbolTable.getLocalVariableStackSize(name);
        stackSize += getStackAlignment(stackSize);
        return stackSize;
    }
    private Map<String, List<Instruction>> generateAssembly(Map<String, QuadList> functionQuads) throws CompileException {
        Map<String, List<Instruction>> generatedAssembly = new HashMap<>();


        List<Integer> cache = new LinkedList<>();
        for(Map.Entry<String, QuadList> functions : functionQuads.entrySet()){
            final String name       = functions.getKey();
            final QuadList quads    = functions.getValue();

            // add prologue
            List<Instruction> instructions = new LinkedList<>(List.of(PROLOGUE_INSTRUCTIONS));

            // create a cache for temporary variables
            // store the maximum amount of space needed for temporary variables
            // allocate that + padding in the end
            // make it so that when we "push" we slot into that variable

            // epilogue
            instructions.addAll(List.of(EPILOGUE_INSTRUCTIONS));

            // calculate stack size and padding
            int stackSize = this.allocateStackSpace(cache, name);
            final int stackAllocationInstructionIndex = PROLOGUE_INSTRUCTIONS.length;
            instructions.add(stackAllocationInstructionIndex, new Instruction(Operation.SUB, new Address(Register.RSP), new Immediate(stackSize)));
            //

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
        Map<String, List<Instruction>> instructions = this.generateAssembly(functionQuads);
        optimizer.optimizeX86Assembly();

        // output assembly
        this.outputX86Assembly(fileName, instructions);
    }

    public void outputX86Assembly(String fileName, Map<String, List<Instruction>> functions){
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
