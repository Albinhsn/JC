package se.liu.albhe576.project;

import java.util.List;
import java.util.Map;

public interface CodeGenerator {
    StringBuilder outputInstructions(Map<String, List<Instruction>> functions);
    List<Instruction> generateInstructions(QuadList quads, String functionName) throws CompileException;
}
