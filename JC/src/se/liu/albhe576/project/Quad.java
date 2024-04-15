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
    public static Symbol getLastOperand(List<Quad> quads){
        return quads.get(quads.size() - 1).operand1;
    }

    public static Quad insertLabel(ResultSymbol label){
        return new Quad(QuadOp.LABEL, label, null, null);
    }

    public Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.result   = result;

    }

    public static void insertJMPOnComparisonCheck(List<Quad> quads, ResultSymbol jmpLocation, boolean jumpIfTrue){
        String immLiteral;
        if(jumpIfTrue){
            immLiteral = "1";
        }else{
            immLiteral = "0";
        }
        ImmediateSymbol immediateSymbol = new ImmediateSymbol(new Token(TokenType.TOKEN_INT_LITERAL, 0, immLiteral));
        ResultSymbol immLoadResult = Compiler.generateResultSymbol();
        quads.add(new Quad(QuadOp.PUSH, null, null, null));
        quads.add(new Quad(QuadOp.LOAD_IMM, immediateSymbol, null, immLoadResult));
        quads.add(new Quad(QuadOp.MOV_REG_CA, null, null, null));
        quads.add(new Quad(QuadOp.POP, null, null, null));
        quads.add(new Quad(QuadOp.CMP, null, null,null));
        quads.add(new Quad(QuadOp.JE, jmpLocation, null, null));

    }

    public String emit(QuadOp prevOp, Map<String, Integer> stackVariables, FunctionSymbol currentFunction){
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
                int offset = 8 * (stackVariables.size() - stackVariables.get(operand1.name) - 1);
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
                if(stackVariables.containsKey(result.name)){
                    int offset = 8 * (stackVariables.size() - stackVariables.get(result.name) - 1);
                    if(offset == 0){
                        return "mov [rsp], rax";
                    }
                   return String.format("mov [rsp + %d], rax", offset);
                }else{
                    stackVariables.put(result.name, stackVariables.size());
                    return "push rax";
                }
            }
            case CMP -> {
                return "cmp rax, rcx";
            }
            case JMP ->{
                return String.format("jmp %s", operand1.name);
            }
            case JNZ ->{
                return String.format("jnz %s", operand1.name);
            }
            case JE->{
                return String.format("je %s", operand1.name);
            }
            case LABEL ->{
                return operand1.name + ":";
            }
            case JZ -> {}
            case SETE -> {
                return "sete al\nmovzx rax, al";
            }
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
                int argSize = 8 * currentFunction.arguments.size();
                return String.format("call %s\nadd rsp, %d", operand1.name, argSize);
            }
            case GET_FIELD -> {}
            case SET_FIELD ->{}
            case RET ->{
                int localVariables = stackVariables.size() - currentFunction.arguments.size();

                if(localVariables > 0){
                    return String.format("add rsp, %d\nret", localVariables * 8);
                }
                return "ret";
            }
            case SHL ->{}
            case SHR -> {}
        }
        return ""; // String.format("UNKNOWN %s", op);
    }
}
