package se.liu.albhe576.project;

import java.util.List;
import java.util.Map;

public interface CodeGenerator {
    StringBuilder outputInstructions(Map<String, List<Instruction>> functions);
    Map<String,List<Instruction>> generateInstructions(Map<String, QuadList> functionQuads) throws CompileException;
}
