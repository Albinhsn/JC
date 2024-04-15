package se.liu.albhe576.project;

import java.util.List;
import java.util.Map;

public class Quad {

    @Override
    public String toString() {
        return op.name() + "\t\t" +
                operand1 + "\t\t" +
                operand2 + "\t\t" +
                result;
    }

    public QuadOp op;
    public Symbol operand1;
    public Symbol operand2;
    public Symbol result;

    public static Symbol getLastResult(List<Quad> quads){
        return quads.get(quads.size() - 1).result;
    }

    public Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.result   = result;

    }

    public String emit(QuadOp prevOp, Map<String, Integer> stackVariables, List<FunctionSymbol> functions){
        switch(this.op){
            case INDEX -> {}
            case LOAD_IMM -> {
                ImmediateSymbol imm = (ImmediateSymbol) operand1;
                if(prevOp == QuadOp.LOAD_IMM || prevOp == QuadOp.LOAD){
                    return String.format("mov rcx, %s", imm.value.literal);
                }
                return String.format("mov rax, %s", imm.value.literal);
            }
            case INC -> {
                return "inc rax";
            }
            case ADD -> {
                return "add rax, rcx";
            }
            case JG -> {}
            case JGE -> {}
            case JL -> {}
            case SUB -> {
                return "sub rax, rcx";
            }
            case MUL -> {
                return "mul rcx";
            }
            case DIV -> {
                return "div rcx";
            }
            case LOAD ->{
                int offset = 8 * (stackVariables.size() - stackVariables.get(operand1.name));
                if(prevOp == QuadOp.LOAD_IMM || prevOp == QuadOp.LOAD){
                    if(offset == 0){
                        return "mov rcx, [rsp]";
                    }
                    return String.format("mov rcx, [rsp + %d]", offset);
                } else if(offset == 0){
                    return "mov rax, [rsp]";
                }
                return String.format("mov rax, [rsp + %d]", offset);

            }
            case STORE -> {
                stackVariables.put(result.name, stackVariables.size());
                return "push rax";
            }
            case CMP -> {}
            case JMP ->{}
            case JNZ ->{}
            case JZ -> {}
            case AND -> {}
            case OR -> {}
            case XOR -> {}
            case PUSH ->{
                stackVariables.put("TEMP", 0);
                return "push rax";
            }
            case POP ->{
                stackVariables.remove("TEMP");
                return "pop rax";
            }
            case MOV_REG_CA ->{
                return "mov rcx, rax";
            }
            case MOV_REG_AC ->{
                return "mov rax, rcx";
            }
            case CALL ->{
                int argSize = 0;
                for(FunctionSymbol functionSymbol :functions){
                    if(functionSymbol.name.equals(operand1.name)){
                        argSize = 8 * functionSymbol.arguments.size();
                    }
                }
                return String.format("call %s\nadd rsp, %d", operand1.name, argSize);
            }
            case GET_FIELD -> {}
            case SET_FIELD ->{}
            case RET ->{
                return "ret";
            }
            case SHL ->{}
            case SHR -> {}
        }
        return ""; // String.format("UNKNOWN %s", op);
    }
}
