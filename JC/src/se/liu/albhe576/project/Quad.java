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


    public Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.result   = result;

    }

    public static void insertBooleanComparison(QuadList quads, String immediateLiteral){
        Symbol immediateSymbol = Compiler.generateImmediateSymbol(DataType.getInt(), immediateLiteral);
        Symbol immLoadResult = Compiler.generateSymbol(DataType.getInt());
        quads.addQuad(QuadOp.PUSH, null, null, Compiler.generateSymbol(DataType.getInt()));
        quads.addQuad(QuadOp.LOAD_IMM, immediateSymbol, null, immLoadResult);
        quads.addQuad(QuadOp.MOV_REG_CA, null, null, null);
        quads.addQuad(QuadOp.POP, null, null, Compiler.generateSymbol(DataType.getInt()));
        quads.addQuad(QuadOp.CMP, null, null,null);
    }

    public static void insertJMPOnComparisonCheck(QuadList quads, Symbol jmpLocation, boolean jumpIfTrue){
        insertBooleanComparison(quads, jumpIfTrue ? "1" : "0");
        quads.addQuad(QuadOp.JE, jmpLocation, null, null);
    }

    public String getRegisterFromType(DataTypes type, int registerIndex){
        final String[] floatRegisters = new String[]{"xmm0", "xmm1"};
        final String[] generalRegisters = new String[]{"rax", "rcx"};
        if(type == DataTypes.FLOAT){
            return floatRegisters[registerIndex];
        }
        return generalRegisters[registerIndex];
    }
    public String setupFloatingPointBinary(){
        StringBuilder builder = new StringBuilder();
        // This means rax is not floating point
        // That means we have to both transfer it to xmm0 and cast it
        if(operand1.type.type != DataTypes.FLOAT){
            builder.append("cvtsi2ss xmm0, rax\n");
        // Same but for rcx
        }else if(operand2.type.type != DataTypes.FLOAT){
            builder.append("cvtsi2ss xmm1, rcx\n");
        }

        return builder.toString();
    }

    public String emit(Stack stack, Quad prevQuad, List<Function> functions, Map<String, Constant> constants) throws UnknownSymbolException {
        QuadOp prevOp = prevQuad == null ? null : prevQuad.op;
        switch(this.op){
            case LOAD_IMM -> {
                ImmediateSymbol imm = (ImmediateSymbol) this.operand1;
                int registerIndex = prevOp != null && prevOp.isLoad() ? 1 :0;
                String register = this.getRegisterFromType(operand1.type.type, registerIndex);

                switch(imm.type.type){
                    case INT -> {return String.format("mov %s, %s", register, imm.value);}
                    case STRING -> {return String.format("mov %s, %s", register,constants.get(imm.value).label);}
                    case FLOAT-> {
                        if(constants.containsKey(imm.value)){
                            return String.format("movss %s,[%s]", register, constants.get(imm.value).label);
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
            case LEA_RSP -> {
                return "lea rax, [rsp]";
            }
            case FADD -> {
                String setup = this.setupFloatingPointBinary();
                return setup + "addss xmm0, xmm1";
            }
            case SUB -> {
                return "sub rax, rcx";
            }
            case FSUB -> {
                String setup = this.setupFloatingPointBinary();
                return setup + "subss xmm0, xmm1";
            }
            case MUL -> {
                return "mul rcx";
            }
            case CVTTSS2SI -> {
                return "cvttss2si rax, xmm0";
            }
            case CVTSI2SS-> {
                return "cvtsi2ss xmm0, rax";
            }
            case FMUL -> {
                String setup = this.setupFloatingPointBinary();
                return setup + "mulss xmm0, xmm1";
            }
            case DIV -> {
                return "idiv rcx";
            }
            case MOD -> {
                return "cdq\nidiv rcx\nmov rax, rdx\n";
            }
            case FDIV -> {
                String setup = this.setupFloatingPointBinary();
                return setup + "divss xmm0, xmm1";
            }
            case LOAD_POINTER ->{
                return stack.loadStructPointer(operand1.name);
            }
            case INDEX ->{
                return "lea rax, [rax + rcx]";
            }
            case DEREFERENCE ->{
                return "mov rax, [rax]";
            }
            case LOAD ->{
                return stack.loadVariable(operand1.name, prevOp);
            }
            case SET_FIELD -> {
                return stack.storeField(operand1.type, operand2);
            }
            case GET_FIELD -> {
                return stack.loadField(operand1.type, operand2.name);
            }
            case STORE -> {
                return stack.storeVariable(result.name);
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
                if(prevQuad.result != null && prevQuad.result.type.type == DataTypes.FLOAT){
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
                if(prevQuad.result.type.type == DataTypes.FLOAT){
                    return "movss xmm0, xmm1";
                }
                return "mov rax, rcx";
            }
            case PUSH_STRUCT ->{
                return stack.pushStruct(operand1);
            }
            case MOV_RDI->{
                return "mov rdi, rax";
            }
            case MOV_RSI->{
                return "mov rsi, rax";
            }
            case MOV_RDX->{
                return "mov rdx, rax";
            }
            case MOV_R8->{
                return "mov r8, rax";
            }
            case MOV_R9->{
                return "mov r9, rax";
            }
            case CALL_LIBRARY -> {
                if(stack.getLocalSize() % 16 != 0){
                    return String.format("sub rsp, 8\ncall %s\nadd rsp, 8", operand1.name);
                }
                return String.format("call %s", operand1.name);
            }
            case CALL ->{
                int argSize = 0;
                for(Function function : functions){
                    if(function.name.equals(operand1.name)){
                        for(StructField field : function.arguments){
                            Struct struct = stack.getStruct(field.type.name);
                            argSize += struct.getSize(stack.structs);
                        }
                        break;
                    }
                }

                if(argSize != 0){
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
