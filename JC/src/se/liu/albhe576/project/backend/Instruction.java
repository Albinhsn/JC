package se.liu.albhe576.project.backend;

/**
 * Token interface for what defines an instruction
 * This is (probably?) very different depending on instruction set and therefore should be defined there
 * But have a function (in this case "emit") in order to get a string version of the instruction to write
 * @see IntelInstruction (for x86 version)
 */
public interface Instruction {
    String emit();
}
