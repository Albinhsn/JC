package se.liu.albhe576.project.backend;

import java.util.Map;

/**
 *  interface for code generation for the language
 *  Used for generating code for a specific instruction set
 * @see InstructionList
 * @see Instruction
 */
public interface CodeGenerator {
    String createOutputString(SymbolTable symbolTable, Map<String, ? extends InstructionList> functions);
    Map<String,? extends  InstructionList> generateInstructions(SymbolTable symbolTable, Map<String, IntermediateList> functionIntermediates) throws CompileException;
}
