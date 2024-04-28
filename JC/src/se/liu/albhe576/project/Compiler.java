package se.liu.albhe576.project;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Compiler {

    private final Map<String, Function> functions;
    private final Map<String, QuadList> functionQuads;
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
    public static int getStackPadding(int stackSize){
        // This is the same as saying (scopeSize == 16 ? 0 : (16 - (scopeSize % 16))
        return (16 - (stackSize % 16)) & 0xF;
    }

    public void compile(String name) throws CompileException, IOException{

        // Intermediate code generation
        for(Map.Entry<String, Function> entry: this.functions.entrySet().stream().filter(x -> !x.getValue().external).toList()){
            Function function = entry.getValue();
            Map<String, VariableSymbol> localSymbols = new HashMap<>();

            QuadList quads = new QuadList();
            this.functionQuads.put(entry.getKey(), quads);

            // IP and RBP
            int offset = 16;
            for(StructField arg : function.getArguments()){
                localSymbols.put(arg.name(), new VariableSymbol(arg.name(), arg.type(), offset));
                offset += symbolTable.getStructSize(arg.type());
            }
            symbolTable.compileFunction(entry.getKey(), localSymbols);
            Stmt.compileBlock(symbolTable, quads, function.getBody());
        }

        // Output intel assembly
        this.generateAssembly(name);
    }

    private static StringBuilder initOutput(Map<String, Constant> constants, Map<String, Function> extern) {
        StringBuilder header = new StringBuilder();
        header.append("global _start\n");

        for(String functionName : extern.keySet()){
            header.append(String.format("extern %s\n", functionName));
        }

        header.append("\n\nsection .data\n");
        for(Map.Entry<String, Constant> entry : constants.entrySet()){
            Constant value = entry.getValue();
            if(value.type() == DataTypes.STRING){
                header.append(String.format("%s db ", value.label()));

                String formatted = entry.getKey().replace("\\n", "\n");
                for (byte b : formatted.getBytes()) {
                    header.append(String.format("%d, ", b));
                }
                header.append("0\n");

            }else{
                header.append(String.format("%s dd %s\n", value.label(), entry.getKey()));
            }
        }
        header.append("\n\nsection .text\n");
        header.append("_start:\n");
        header.append("call main\n");
        header.append("mov rbx, rax\n");
        header.append("mov rax, 1\n");
        header.append("int 0x80\n");

        return header;
    }

    private void handleStackAlignment(StringBuilder stringBuilder,  String functionName) {
        int scopeSize = this.symbolTable.getLocalVariableStackSize(functionName);
        scopeSize += getStackPadding(scopeSize);
        if(scopeSize != 0){
            stringBuilder.append(String.format("sub rsp, %d\n", scopeSize));
        }
    }
    private void outputFunctionBody(StringBuilder stringBuilder, QuadList quads) throws CompileException {
        final Map<String, Constant> constants   = this.symbolTable.getConstants();
        final Map<String, Struct> structs       = this.symbolTable.getStructs();

        List<Instruction> functionInstruction = new ArrayList<>();
        for (Quad intermediate : quads) {
            Instruction[] instruction = intermediate.emitInstruction(functions, constants, structs);
            functionInstruction.addAll(Arrays.stream(instruction).toList());
        }

        if(quads.isEmpty() || quads.getLastOp() != QuadOp.RET){
            Quad retQuad = new Quad(QuadOp.RET, null, null, null);
            Instruction[] instruction = retQuad.emitInstruction(functions, constants, structs);
            functionInstruction.addAll(Arrays.stream(instruction).toList());
        }

        LinkedList<Instruction> llInstructions = new LinkedList<>(functionInstruction);
        this.optimizeFunction(llInstructions);

        for(int i = 0; i < llInstructions.size(); i++){
            Instruction instruction = llInstructions.get(i);
            stringBuilder.append(instruction.emit()).append("\n");
        }

    }

    public void generateAssembly(String name) throws IOException, CompileException {
        final Map<String, Function> functions = this.symbolTable.getInternalFunctions();
        StringBuilder stringBuilder = initOutput(this.symbolTable.getConstants(), this.symbolTable.getExternalFunctions());

        for (String key : functions.keySet()) {
            stringBuilder.append(String.format("\n\n%s:\npush rbp\nmov rbp, rsp\n", key));
            this.handleStackAlignment(stringBuilder, key);
            this.outputFunctionBody(stringBuilder, this.functionQuads.get(key));
        }
        System.out.printf("Finished optimizing total: %d, removed: %d\n",totalSize, removed);

        FileWriter fileWriter = new FileWriter(name);
        fileWriter.write(stringBuilder.toString());
        fileWriter.flush();
        fileWriter.close();
    }
    public int removed = 0;
    public int totalSize = 0;

    private boolean optimizePush(LinkedList<Instruction> instructions, int start){
        Instruction curr = instructions.get(start);
        Instruction prev;
        int j = start + 2;
        while(j < instructions.size()){
            prev = curr;
            curr = instructions.get(j);
            if(curr.op.isJumping()){
                return false;
            }
            if(prev.op == Operation.SUB && prev.operand0.isRSP() && curr.op == Operation.PUSH){
                return false;
            }
            else if(prev.op == Operation.POP && prev.operand0.isPrimary() && curr.op == Operation.ADD && curr.operand0.isRSP()){
                System.out.printf("Removed %d: %s, %d: %s\n", start,instructions.get(start).emit(), j, instructions.get(j).emit());
                instructions.remove(j);
                instructions.remove(start);
                return true;
            }
            j++;
        }
        return false;
    }

    private boolean optimizeSignExtend(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 1){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.MOVSX || !first.operand0.isPrimary() || !first.operand1.isPrimary()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.MOV || !second.operand1.isPrimary() || !second.operand1.equals(first.operand1)){
            return false;
        }

        instructions.remove(i + 1);
        instructions.remove(i);
        instructions.add(i, new Instruction(Operation.MOV, second.operand0, first.operand1));

        return true;
    }

    private boolean optimizeSignExtendImmediateMove(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 1){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.MOV || !first.operand0.isPrimary() || !first.operand1.isImmediate()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.MOVSX || !second.operand0.isPrimary() || !second.operand1.isPrimary()){
            return false;
        }

        instructions.remove(i + 1);
        instructions.remove(i);
        instructions.add(i, new Instruction(Operation.MOV, second.operand0, first.operand1));

        return true;
    }

    private boolean optimizeSetupBinary(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 3){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.PUSH || !first.operand0.isPrimary()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(!(second.op == Operation.MOV || second.op == Operation.LEA) || !second.operand0.isSecondary()){
            return false;
        }
        Instruction third = instructions.get(i + 2);
        if(third.op != Operation.POP || !third.operand0.isPrimary()){
            return false;
        }
        instructions.remove(i + 2);
        instructions.remove(i);

        return true;
    }

    private boolean optimizeIndexing(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 3){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.MOV || !first.operand0.isSecondary() || !first.operand1.isImmediate()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.LEA || !second.operand0.isPrimary() || !second.operand1.isStackPointer()){
            return false;
        }
        Instruction third = instructions.get(i + 2);
        if(third.op != Operation.ADD || !third.operand0.isSecondary() || !third.operand1.isPrimary()){
            return false;
        }

        Register stackPointer = (Register) second.operand1;
        Immediate firstImm = (Immediate)first.operand1;
        int additionalOffset = Integer.parseInt(firstImm.immediate);
        instructions.remove(i + 2);
        instructions.remove(i + 1);
        instructions.remove(i);

        instructions.add(i, new Instruction(Operation.LEA, third.operand0, new Register(stackPointer.type, stackPointer.offset + additionalOffset, true)));

        return true;
    }

    private boolean optimizePointerArithmetic(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 3){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.MOV || !first.operand0.isPrimary() || !first.operand1.isImmediate()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.IMUL || !second.operand0.isPrimary() || !second.operand1.isImmediate()){
            return false;
        }
        Instruction third = instructions.get(i + 2);
        if(third.op != Operation.MOV || !third.operand0.isSecondary() || !third.operand1.isPrimary()){
            return false;
        }

        instructions.remove(i + 2);
        instructions.remove(i + 1);
        instructions.remove(i);
        Immediate fstImm = (Immediate) first.operand1;
        Immediate sndImm = (Immediate) second.operand1;

        int newImmValue = (Integer.parseInt(fstImm.immediate) * Integer.parseInt(sndImm.immediate));
        instructions.add(i, new Instruction(Operation.MOV, third.operand0, new Immediate(String.valueOf(newImmValue))));

        return true;
    }

    private boolean optimizeStore(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 2){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.LEA || !first.operand0.isSecondary() || !first.operand1.isStackPointer()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.MOV || !second.operand0.isSecondaryEffective() || !second.operand1.isPrimary()){
            return false;
        }

        instructions.remove(i + 1);
        instructions.remove(i);

        instructions.add(i, new Instruction(Operation.MOV, first.operand1, second.operand1));

        return true;
    }

    private boolean optimizeMoveStruct(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 4){
            return false;
        }
        Instruction first = instructions.get(i);
        if(!(first.op == Operation.LEA || first.op.isMove()) || !first.operand0.isPrimary() || !first.operand1.isStackPointer()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(!(second.op == Operation.LEA || second.op.isMove()) || !second.operand0.isSecondary() || !second.operand1.isStackPointer()){
            return false;
        }
        Instruction third = instructions.get(i + 2);
        if(third.op != Operation.MOV || !third.operand1.isPrimary()){
            return false;
        }
        Instruction fourth = instructions.get(i + 3);
        if(fourth.op != Operation.MOV || !fourth.operand1.isSecondary()){
            return false;
        }

        instructions.remove(i + 3);
        instructions.remove(i + 2);
        instructions.remove(i + 1);
        instructions.remove(i);

        instructions.add(i, new Instruction(Operation.LEA, third.operand0, first.operand1));
        instructions.add(i + 1, new Instruction(Operation.LEA, fourth.operand0, second.operand1));

        return true;

    }
    private boolean optimizeLoadFloat(LinkedList<Instruction> instructions, int i) {
        if (instructions.size() <= i + 2) {
            return false;
        }

        Instruction first = instructions.get(i);
        if (first.op != Operation.LEA || !first.operand0.isPrimary()) {
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(!second.op.isMove() || !second.operand0.isPrimaryFloat() || !second.operand1.isPrimaryEffective()){
            return false;
        }

        instructions.remove(i + 1);
        instructions.remove(i);
        instructions.add(i, new Instruction(second.op, second.operand0, first.operand1));
        return true;
    }

    private boolean optimizeAddImmediate(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 2){
            return false;
        }

        Instruction first = instructions.get(i);
        if(first.op != Operation.MOV  || !first.operand0.isSecondary() || !first.operand1.isImmediate()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.ADD || !(second.operand0.isSecondary() || second.operand0.isPrimary()) || !(second.operand1.isPrimary() || second.operand1.isSecondary())){
            return false;
        }

        Immediate immediate = (Immediate) first.operand1;
        instructions.remove(i + 1);
        instructions.remove(i);
        if(Integer.parseInt(immediate.immediate) != 0){
            instructions.add(i, new Instruction(second.op, second.operand0, immediate));
        }
        return true;
    }

    private boolean optimizeLoadEffective(LinkedList<Instruction> instructions, int i){

        if(instructions.size() <= i + 2){
            return false;
        }

        Instruction first = instructions.get(i);
        if(first.op != Operation.LEA || !first.operand0.isPrimary() || !first.operand1.isStackPointer()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(!(second.op.isMove() || second.op == Operation.LEA) || !second.operand1.isPrimaryPointer()){
            return false;
        }

        Register reg0 = (Register) first.operand1;
        Register reg1 = (Register) second.operand1;

        int newEffective = reg0.offset + reg1.offset;
        instructions.remove(i + 1);
        instructions.remove(i);

        instructions.add(i, new Instruction(second.op, second.operand0, new Register(reg0.type, newEffective, true)));
        return true;
    }
    private boolean optimizeLoad(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 2){
            return false;
        }

        Instruction first = instructions.get(i);
        if(!(first.op == Operation.MOV || first.op == Operation.LEA) || !first.operand0.isPrimary()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.MOV || !second.operand0.isSecondary() || !second.operand1.isPrimary()){
            return false;
        }

        instructions.remove(i + 1);
        instructions.remove(i);
        instructions.add(i, new Instruction(first.op, second.operand0, first.operand1));
        return true;
    }

    private void  optimizeFunction(LinkedList<Instruction> instructions){
        int prior = instructions.size();
        totalSize += prior;
        int i = 0;
        while(i < instructions.size() - 1){

            Instruction instruction = instructions.get(i);
            Instruction nextInstruction = instructions.get(i + 1);
            if(instruction.label != null){
                ++i;
                continue;
            }
            if(instruction.op == Operation.LEA && instruction.operand0.equals(new Register(RegisterType.RAX)) && instruction.operand1.equals(new Register(RegisterType.RAX))){
                instructions.remove(i);
                i = 0;
            }
            else if(instruction.op == Operation.LEA && instruction.operand0.isPrimary() && nextInstruction.op == Operation.MOV && nextInstruction.operand0.isPrimary() && nextInstruction.operand1.isPrimaryEffective()){
                instructions.remove(i + 1);
                instructions.remove(i);
                instructions.add(i, new Instruction(Operation.MOV, nextInstruction.operand0, instruction.operand1));
                i = 0;
            }
            else if(optimizeLoad(instructions, i) || optimizeLoadFloat(instructions, i)){
                i = 0;
            }
            else if(instruction.op == Operation.SUB && instruction.operand0.isRSP() && nextInstruction.op == Operation.PUSH){
                if(!optimizePush(instructions, i)){
                    i++;
                }else{
                    i = 0;
                }
            }else if(optimizePointerArithmetic(instructions, i)){
               i = 0;
            }else if(optimizeMoveStruct(instructions, i)){
                i = 0;
           }else if(optimizeIndexing(instructions, i)) {
                i = 0;
            }else if(optimizeSignExtendImmediateMove(instructions, i)) {
                i = 0;
            }else if(optimizeSignExtend(instructions, i)){
                i = 0;
            }else if(optimizeLoadEffective(instructions, i)){
                i = 0;
            }else if(optimizeSetupBinary(instructions, i)) {
                i = 0;
            }else if(optimizeStore(instructions, i)){
               i = 0;
            }else if(optimizeAddImmediate(instructions, i)){
                i = 0;
            }
            else if(instruction.op == Operation.ADD && instruction.operand0.isPrimary() && instruction.operand1.isSecondary() && nextInstruction.op == Operation.MOV && nextInstruction.operand0.isSecondary() && nextInstruction.operand1.isPrimary()){
                instructions.remove(i + 1);
                instructions.remove(i);
                instructions.add(i, new Instruction(Operation.ADD, instruction.operand1, instruction.operand0));
                i = 0;
            }
            else if((instruction.op == Operation.ADD || instruction.op == Operation.SUB) && (nextInstruction.op == Operation.SUB || nextInstruction.op == Operation.ADD) && instruction.operand0.isRSP() && nextInstruction.operand0.isRSP()){
                Immediate fstImm = (Immediate) instruction.operand1;
                Immediate sndImm = (Immediate) nextInstruction.operand1;

                int fstSize = Integer.parseInt(fstImm.immediate);
                fstSize = instruction.op == Operation.ADD ? fstSize : -fstSize;
                int sndSize = Integer.parseInt(sndImm.immediate);
                sndSize = nextInstruction.op == Operation.ADD ? sndSize : -sndSize;
                int newSize = fstSize + sndSize;

                Immediate newImm = new Immediate(String.valueOf(Math.abs(newSize)));
                Operation newOp = newSize < 0 ? Operation.SUB : Operation.ADD;

                instructions.remove(i + 1);
                instructions.remove(i);
                if(newSize != 0){
                    instructions.add(i, new Instruction(newOp, nextInstruction.operand0, newImm));
                }
                i = 0;
            }else{
                i++;
            }
        }


        int current = instructions.size();
        removed += prior - current;
        System.out.printf("Finished optimizing, removed %d, %d -> %d\n", prior - current, current, prior);

    }


    public Compiler(Map<String, Struct> structs, Map<String, Function> functions){
        this.functions                  = functions;
        this.functionQuads              = new HashMap<>();
        Map<String, Constant> constants = new HashMap<>();
        this.symbolTable                = new SymbolTable(structs, constants, functions);
    }
}
