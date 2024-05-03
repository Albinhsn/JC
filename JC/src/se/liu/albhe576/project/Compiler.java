package se.liu.albhe576.project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {
    private final CodeGenerator codeGenerator;
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

            symbolTable.addArguments(localSymbols, function);
            symbolTable.addFunction(functionEntry.getKey(), localSymbols);
            Stmt.compileBlock(symbolTable, quads, function.getBody());
            functionIntermediates.put(functionEntry.getKey(), quads);

            System.out.printf("%s:\n\n", functionEntry.getKey());
            for(Quad quad : quads){
                    System.out.println(quad);
            }
        }
        return functionIntermediates;
    }
    public static int getStackAlignment(int stackSize){
        final int alignmentInBytes = 16;
        return (alignmentInBytes - (stackSize % alignmentInBytes)) & (alignmentInBytes - 1);
    }
    public void compile(String fileName) throws CompileException, IOException{
        // Intermediate code generation
        final Map<String, QuadList> functionQuads = this.generateIntermediate();

        Optimizer.optimizeIntermediates(functionQuads.values());

        Map<String, List<Instruction>> functionInstructions = new HashMap<>();
        for(Map.Entry<String, QuadList> function : functionQuads.entrySet()){
            List<Instruction> instructions = this.codeGenerator.generateInstructions(function.getValue(), function.getKey());
            functionInstructions.put(function.getKey(), instructions);
        }
        StringBuilder output = this.codeGenerator.outputInstructions(functionInstructions);

        FileWriter fileWriter = new FileWriter(fileName);
        fileWriter.write(output.toString());
        fileWriter.flush();
        fileWriter.close();

    }

    public Compiler(SymbolTable symbolTable, CodeGenerator codeGenerator){
        this.symbolTable                = symbolTable;
        this.codeGenerator              = codeGenerator;
    }
}
