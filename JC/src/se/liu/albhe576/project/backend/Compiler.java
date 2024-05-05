package se.liu.albhe576.project.backend;

import se.liu.albhe576.project.frontend.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * The main class for the backend of the language
 * Generates intermediates and outputs assembly via the given code generator
 * @see CodeGenerator
 * @see SymbolTable
 * @see IntermediateList
 * @see Instruction
 */
public class Compiler {
    private final CodeGenerator codeGenerator;
    private final SymbolTable symbolTable;
    private final Logger logger;
    public static void panic(String msg, int line, String filename) {
        System.out.printf("%s:%d[%s]", filename,line,msg);
        System.exit(1);
    }

    public Map<String, IntermediateList> generateIntermediate() throws CompileException {
        final Map<String, IntermediateList> functionIntermediates = new HashMap<>();
        // Compile intermediates for every function
        for(Map.Entry<String, Function> functionEntry: this.symbolTable.getInternalFunctions().entrySet()){
            final Function function                                 = functionEntry.getValue();
            final Map<String, VariableSymbol> localSymbols          = new HashMap<>();
            final IntermediateList intermediates                    = new IntermediateList();

            symbolTable.addArguments(localSymbols, function);
            symbolTable.addFunction(functionEntry.getKey(), localSymbols);
            Statement.compileBlock(symbolTable, intermediates, function.getBody());
            functionIntermediates.put(functionEntry.getKey(), intermediates);
        }
        return functionIntermediates;
    }
    public static int getStackAlignment(int stackSize){
        // The stack needs to be aligned to 16 bytes when calling external functions on 64 bit linux (libc etc)
        // So we pad the stack up to that amount
        final int alignmentInBytes = 16;
        // Since the alignment is a power of two in bytes we can do this to avoid a branch
        // (alignmentInBytes - 1) is then just filled with 1's
        // And doing an & on it will essentially being mod 16 in this case
        return (alignmentInBytes - (stackSize % alignmentInBytes)) & (alignmentInBytes - 1);
    }
    private void debugFunctions(){
        for(Map.Entry<String, Function> functionEntry : this.symbolTable.getInternalFunctions().entrySet()){
            Function function = functionEntry.getValue();
            String log = String.format(
                    "function %s, returns: %s, arguments: %d, statements:%d ",
                    functionEntry.getKey(),
                    function.getReturnType(),
                    function.getArguments().size(),
                    function.getBody().size()
            );
            this.logger.info(log);
        }
    }
    private void debugIntermediates(Map<String, IntermediateList> functionIntermediates){
        for(Map.Entry<String, IntermediateList> functionEntry : functionIntermediates.entrySet()){
            IntermediateList function = functionEntry.getValue();
            String log = String.format("intermediates for %s: %d", functionEntry.getKey(), function.size());
            for(Intermediate intermediate : function){
                this.logger.severe(intermediate.toString());
            }
            this.logger.info(log);
        }
    }
    private void debugInstructions(Map<String, ? extends  InstructionList> instructions){
        this.logger.info(String.format("Compiled %d constants", symbolTable.getConstants().size()));
        this.logger.info(String.format("Compiled %d external functions", symbolTable.getExternalFunctions().size()));
        for(Map.Entry<String, ? extends InstructionList> function : instructions.entrySet()){
            this.logger.info(String.format("%s generated %d instructions", function.getKey(), function.getValue().size()));
        }
    }
    public void compile(String fileName) throws CompileException, IOException{
        final Map<String, IntermediateList> functionIntermediates   = this.generateIntermediate();
        this.debugFunctions();

        // Generate the instructions for every function
        Map<String, ? extends InstructionList> functionInstructions = this.codeGenerator.generateInstructions(symbolTable,functionIntermediates);
        this.debugIntermediates(functionIntermediates);

        // Get the output string for the instructions
        String output = this.codeGenerator.createOutputString(symbolTable, functionInstructions);
        this.debugInstructions(functionInstructions);

        // Write it to a file
        FileWriter fileWriter = new FileWriter(fileName);
        fileWriter.write(output);
        fileWriter.flush();
        fileWriter.close();
    }

    public Compiler(SymbolTable symbolTable, CodeGenerator codeGenerator, FileHandler fileHandler){
        this.symbolTable                = symbolTable;
        this.codeGenerator              = codeGenerator;
        this.logger                     = Logger.getLogger("Compiler");
        this.logger.addHandler(fileHandler);
    }
}
