package se.liu.albhe576.project;

import java.nio.channels.FileLock;
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
    public static Symbol getLastOperand1(List<Quad> quads){
        return quads.get(quads.size() - 1).operand1;
    }

    public static Quad insertLabel(Symbol label){
        return new Quad(QuadOp.LABEL, label, null, null);
    }

    public Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.result   = result;

    }


    public static void insertBooleanComparison(List<Quad> quads, String immediateLiteral){
        Symbol immediateSymbol = Compiler.generateImmediateSymbol(DataType.getInt(), immediateLiteral);
        Symbol immLoadResult = Compiler.generateSymbol(DataType.getInt());
        quads.add(new Quad(QuadOp.PUSH, null, null, null));
        quads.add(new Quad(QuadOp.LOAD_IMM, immediateSymbol, null, immLoadResult));
        quads.add(new Quad(QuadOp.MOV_REG_CA, null, null, null));
        quads.add(new Quad(QuadOp.POP, null, null, null));
        quads.add(new Quad(QuadOp.CMP, null, null,null));
    }

    public static void insertJMPOnComparisonCheck(List<Quad> quads, Symbol jmpLocation, boolean jumpIfTrue){
        insertBooleanComparison(quads, jumpIfTrue ? "1" : "0");
        quads.add(new Quad(QuadOp.JE, jmpLocation, null, null));
    }

    public String getRegisterFromType(DataTypes type, int registerIndex){
        final String[] floatRegisters = new String[]{"xmm0", "xmm1"};
        final String[] generalRegisters = new String[]{"rax", "rcx"};
        if(type == DataTypes.FLOAT){
            return floatRegisters[registerIndex];
        }
        return generalRegisters[registerIndex];
    }

    public String emit(Stack stack, Quad prevQuad, List<Function> functions, Map<String, String> constants) throws UnknownSymbolException {
        QuadOp prevOp = prevQuad == null ? null : prevQuad.op;
        switch(this.op){
            case LOAD_IMM -> {
                ImmediateSymbol imm = (ImmediateSymbol) this.operand1;
                if(prevOp == QuadOp.LOAD_IMM || prevOp == QuadOp.LOAD){
                    return String.format("mov %s, %s", getRegisterFromType(operand1.type.type,1), imm.value);
                }

                switch(imm.type.type){
                    case INT -> {return String.format("mov rax, %s", imm.value);}
                    case FLOAT-> {
                        if(constants.containsKey(imm.value)){
                            return String.format("movss xmm0,[%s]", constants.get(imm.value));
                        }
                        throw new UnknownSymbolException(String.format("Couldn't find constant '%s'", imm.value));
                    }
                }
                throw new UnknownSymbolException(String.format("Can't load this type? %s", imm.type.type));
            }
            case INC -> {
                return "inc rax";
            }
            case DEC -> {
                return "dec rax";
            }
            case ADD -> {
                return "add rax, rcx";
            }
            case FADD -> {
                return "addss xmm0, xmm1\nmovq rax, xmm0";
            }
            case SUB -> {
                return "sub rax, rcx";
            }
            case FSUB -> {
                return "subss xmm0, xmm1\nmovq rax, xmm0";
            }
            case MUL -> {
                return "mul rcx";
            }
            case CVTTSS2SI -> {
                return "movq xmm0, rax\ncvttss2si rax, xmm0";
            }
            case CVTDQ2PD-> {
                return "movq xmm0, rax\ncvtdq2pd xmm0, xmm0\nmovq rax, xmm0";
            }
            case FMUL -> {
                return "mulss xmm0, xmm1\nmovq rax, xmm0";
            }
            case DIV -> {
                return "idiv rcx";
            }
            case MOD -> {
                return "cdq\nidiv rcx\nmov rax, rdx\n";
            }
            case FDIV -> {
                return "divss xmm0, xmm1\nmovq rax, xmm0";
            }
            case LOAD_POINTER ->{
                return stack.loadStructPointer(operand1.name);
            }
            case INDEX ->{
                return "mov rax, [rax + rcx]";
            }
            case DEREFERENCE ->{
                return "mov rax, [rax]";
            }
            case LOAD ->{
                return stack.loadVariable(operand1.name, prevOp);
            }
            case SET_FIELD -> {
                return stack.storeField(result, operand2);
            }
            case GET_FIELD -> {
                return stack.loadField(operand1.name, operand2.name);
            }
            case STORE -> {
                return stack.storeVariable(result.type, result.name);
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
            case SETE -> {
                return "sete al\nmovzx rax, al";
            }
            case SETLE -> {
                return "setle al\nmovzx rax, al";
            }
            case SETG -> {
                return "setg al\nmovzx rax, al";
            }
            case SETNE -> {
                return "setne al\nmovzx rax, al";
            }
            case SETGE -> {
                return "setge al\nmovzx rax, al";
            }
            case SETL -> {
                return "setl al\nmovzx rax, al";
            }
            case PUSH ->{
                if(prevQuad.result.type.type == DataTypes.FLOAT){
                    return "sub rsp, 8\nmovss [rsp], xmm0";
                }
                return "push rax";
            }
            case POP ->{
                if(this.result.type.type == DataTypes.FLOAT){
                    return "movss xmm0, [rsp]\nadd rsp, 8";
                }
                return "pop rax";
            }
            case MOV_REG_CA ->{
                if(prevQuad.result.type.type == DataTypes.FLOAT){
                    return "movss xmm1, xmm0";
                }
                return "mov rcx, rax";
            }
            case MOV_REG_AC ->{
                return "mov rax, rcx";
            }
            case MOV_REG_DA ->{
                return "mov rdx, rax";
            }
            case MOV_REG_AD ->{
                return "mov rax, rdx";
            }
            case MOV_RDI->{
                return "mov rdi, rax";
            }
            case CALL ->{
                int argSize = 1;
                for(Function function : functions){
                    if(function.name.equals(operand1.name)){
                        argSize *= function.arguments.size();
                        break;
                    }
                }

                if(argSize != 1){
                    return String.format("call %s\nadd rsp, %d", operand1.name, argSize);
                }
                return String.format("call %s", operand1.name);
            }
            case LOGICAL_NOT ->{
                return "xor rax, 1";
            }
            case NOT ->{
                return "not rax\ninc rax";
            }
            case RET ->{
                int localVariables = stack.getLocalSize();
                if(localVariables > 0){
                    return "mov rsp, rbp\npop rbp\nret";
                }
                return "mov rsp, rbp\npop rbp\nret";
            }
        }
        throw new UnknownSymbolException(String.format("Don't know how to do %s", op));
    }
}
