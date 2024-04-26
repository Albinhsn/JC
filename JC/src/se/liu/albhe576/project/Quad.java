package se.liu.albhe576.project;

import java.util.Map;

public record Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result) {
    private static int getFunctionArgumentsStackSize(String name, Map<String, Function> functions, Stack stack) {
        Function function = functions.get(name);
        if (function.external) {
            return 0;
        }

        int argSize = 0;
        if (function.getArguments() != null) {
            Map<String, Struct> structs = stack.getStructs();
            for (StructField field : function.getArguments()) {
                argSize += SymbolTable.getStructSize(structs, field.type());
            }
        }
        return argSize;
    }

    private static final String[] FLOAT_REGISTERS = new String[]{"xmm0", "xmm1", "xmm2"};
    private static final String[] INTEGER_REGISTERS = new String[]{"eax", "ecx", "ebx"};
    private static final String[] BYTE_REGISTER = new String[]{"al", "cl", "bl"};
    private static final String[] SHORT_REGISTER = new String[]{"ax", "cx", "bx"};
    private static final String[] LONG_REGISTERS = new String[]{"rax", "rcx", "rbx"};

    public static String getRegisterFromType(DataType type, int registerIndex) {
        if (type.isFloat() || type.isDouble()) {
            return FLOAT_REGISTERS[registerIndex];
        } else if (type.isByte()) {
            return BYTE_REGISTER[registerIndex];
        } else if (type.isShort()) {
            return SHORT_REGISTER[registerIndex];
        } else if (type.isInteger()) {
            return INTEGER_REGISTERS[registerIndex];
        }
        return LONG_REGISTERS[registerIndex];
    }

    public static String getMovOpFromType(DataType type) {
        if(type.isFloat()){
            return "movss";
        }
        return type.isDouble() ? "movsd" : "mov";
    }
    public static StringPair getMovOpAndRegisterFromType(DataType type, int idx){
        return new StringPair(getMovOpFromType(type), getRegisterFromType(type, idx));
    }
    public static String getConstantStringLocation(Map<String, Constant> constants, DataType type, String label){
        if(type.isFloat()){
            return String.format("[%s]", constants.get(label).label());
        }else if(type.type == DataTypes.STRING){
            return String.format("%s", constants.get(label).label());
        }
        return label;
    }
    private String loadImmediate(Map<String, Constant> constants){
        ImmediateSymbol imm = (ImmediateSymbol) this.operand1;
        String immValue = getConstantStringLocation(constants, imm.type, imm.getValue());
        StringPair movePair = getMovOpAndRegisterFromType(imm.type, 0);

        return String.format("%s %s, %s", movePair.move(), movePair.register(), immValue);
    }

    public String emit(Stack stack, Map<String, Function> functions, Map<String, Constant> constants) throws CompileException {
        switch (this.op) {
            case LOAD_IMM -> {return this.loadImmediate(constants);}
            case INC -> {
                if (operand1.type.isFloat()) {
                    return "mov rcx, 1\ncvtsi2ss xmm1, rcx\naddss xmm0, xmm1";
                }
                if (operand1.type.isDouble()) {
                    return "mov rcx, 1\ncvtsi2sd xmm1, rcx\naddsd xmm0, xmm1";
                }
                return "inc rax";
            }
            case DEC -> {
                if (operand1.type.isDouble()) {
                    return "mov rcx, 1\ncvtsi2sd xmm1, rcx\nsubsd xmm0, xmm1";
                }
                if (operand1.type.isFloat()) {
                    return "mov rcx, 1\ncvtsi2ss xmm1, rcx\nsubss xmm0, xmm1";
                }
                if (operand1.type.isInteger()) {
                    return "dec eax";
                }
                return "dec rax";
            }
            case IMUL -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) this.operand1;
                return String.format("imul rax, %s", immediateSymbol.getValue());
            }
            case ADD -> {
                String reg0 = getRegisterFromType(result.type, 0);
                String reg1 = getRegisterFromType(result.type, 1);
                if (result.type.isDouble()) {
                    return String.format("addsd %s, %s", reg0, reg1);
                }
                if (result.type.isFloat()) {
                    return String.format("addss %s, %s", reg0, reg1);
                }
                return String.format("add %s, %s", reg0, reg1);
            }
            case SUB -> {
                String reg0 = getRegisterFromType(result.type, 0);
                String reg1 = getRegisterFromType(result.type, 1);
                if (result.type.isDouble()) {
                    return String.format("subsd %s, %s", reg0, reg1);
                }
                if (result.type.isFloat()) {
                    return String.format("subss %s, %s", reg0, reg1);
                }
                return String.format("sub %s, %s", reg0, reg1);
            }
            case MUL -> {
                String reg0 = getRegisterFromType(result.type, 0);
                String reg1 = getRegisterFromType(result.type, 1);
                if (result.type.isDouble()) {
                    return String.format("mulsd %s, %s", reg0, reg1);
                }
                if (result.type.isFloat()) {
                    return String.format("mulss %s, %s", reg0, reg1);
                }
                return String.format("mul %s", reg1);
            }
            case DIV -> {
                String reg0 = getRegisterFromType(result.type, 0);
                String reg1 = getRegisterFromType(result.type, 1);
                if (result.type.isDouble()) {
                    return String.format("divsd %s, %s", reg0, reg1);
                }
                if (result.type.isFloat()) {
                    return String.format("divss %s, %s", reg0, reg1);
                }
                return String.format("xor rdx, rdx\nidiv %s", reg1);
            }
            case MOD -> {
                return "cdq\nxor rdx, rdx\nidiv rcx\nmov rax, rdx\n";
            }
            case LOAD_VARIABLE_POINTER -> {
                VariableSymbol variableSymbol = (VariableSymbol) operand1;
                return stack.loadVariablePointer(variableSymbol.id);
            }
            case LOAD_POINTER -> {return "lea rax, [rax]";}
            case LOAD_FIELD_POINTER -> {
                VariableSymbol variable = (VariableSymbol) operand1;
                return stack.loadFieldPointer(variable.id, operand2.name);
            }
            case DEREFERENCE, INDEX -> {
                if (result.type.isStruct() && operand2.type.isPointer()) {
                    return "lea rax, [rax]";
                }
                StringPair movePair = getMovOpAndRegisterFromType(result.type, 0);
                return String.format("%s %s, [rax]", movePair.move(), movePair.register());
            }
            case LOAD -> {
                VariableSymbol variableSymbol = (VariableSymbol) operand1;
                String out = stack.loadVariable(variableSymbol.id);
                if(operand1.type.isByte()){
                    return out + "\nmovsx rax, al";
                }
                if(operand1.type.isShort()){
                    return out + "\nmovsx rax, ax";
                }
                return out;
            }
            case SET_FIELD -> {return stack.storeField(operand2.type, operand1);}
            case GET_FIELD -> {return stack.loadField(operand1.type, operand2.name);}
            case STORE -> {
                VariableSymbol variableSymbol = (VariableSymbol) operand1;
                return stack.storeVariable(variableSymbol.id);
            }
            case STORE_INDEX -> {
                StringPair movePair = getMovOpAndRegisterFromType(operand1.type, 0);
                return String.format("%s [rcx], %s", movePair.move(), movePair.register());
            }
            case CMP -> {
                if (operand1 != null && operand1.type.isDouble()) {
                    return "comisd xmm0, xmm1";
                }
                if (operand1 != null && operand1.type.isFloat()) {
                    return "comiss xmm0, xmm1";
                }
                if (operand1 != null && operand1.type.isInteger()) {
                    return "cmp eax, ecx";
                }
                if (operand1 != null && operand1.type.isShort()) {
                    return "cmp ax, cx";
                }
                if (operand1 != null && operand1.type.isByte()) {
                    return "cmp al, cl";
                }

                return "cmp rax, rcx";
            }
            case JMP -> {return String.format("jmp %s", operand1.name);}
            case JNZ -> {return String.format("jnz %s", operand1.name);}
            case JE -> {return String.format("je %s", operand1.name);}
            case JL -> {return String.format("jl %s", operand1.name);}
            case JLE -> {return String.format("jle %s", operand1.name);}
            case JG -> {return String.format("jg %s", operand1.name);}
            case JGE -> {return String.format("jge %s", operand1.name);}
            case JA -> {return String.format("ja %s", operand1.name);}
            case JAE -> {return String.format("jae %s", operand1.name);}
            case JB -> {return String.format("jb %s", operand1.name);}
            case JBE -> {return String.format("jbe %s", operand1.name);}
            case JNE -> {return String.format("jne %s", operand1.name);}
            case LABEL -> {return operand1.name + ":";}
            case SETLE -> {return "setle al\nmovsx rax, al";}
            case SETG -> {return "setg al\nmovsx rax, al";}
            case SETGE -> {return "setge al\nmovsx rax, al";}
            case SETL -> {return "setl al\nmovsx rax, al";}
            case SETE -> {return "sete al\nmovsx rax, al";}
            case SETA -> {return "seta al\nmovsx rax, al";}
            case SETNE -> {return "setne al\nmovsx rax, al";}
            case SETAE -> {return "setae al\nmovsx rax, al";}
            case SETB -> {return "setb al\nmovsx rax, al";}
            case SETBE -> {return "setbe al\nmovsx rax, al";}
            case CONVERT_DOUBLE_TO_FLOAT-> {return "cvtsd2ss xmm0, xmm0";}
            case CONVERT_DOUBLE_TO_LONG-> {return "cvttsd2si rax, xmm0";}
            case CONVERT_FLOAT_TO_DOUBLE -> {return "cvtss2sd xmm0, xmm0";}
            case CONVERT_FLOAT_TO_INT-> {return "cvtss2si eax, xmm0";}
            case CONVERT_INT_TO_FLOAT -> {return "cvtsi2ss xmm0, eax";}
            case CONVERT_LONG_TO_DOUBLE -> {return "cvtsi2sd xmm0, rax";}
            case ZX_SHORT -> {return "movsx rax, ax";}
            case ZX_INT-> {return "movsx rax, eax";}
            case ZX_BYTE -> {return "movsx rax, al";}
            case SHL -> {return "shl rax, cl";}
            case AND -> {return "and rax, rcx";}
            case OR -> {return "or rax, rcx";}
            case XOR -> {return "xor rax, rcx";}
            case SHR -> {return "shr rax, cl";}
            case PUSH_RCX -> {
                return "push rcx";
            }
            case POP_RCX -> {
                return "pop rcx";
            }
            case PUSH -> {
                if (this.result.type.isFloat()) {
                    return "sub rsp, 4\nmovss [rsp], xmm0";
                }
                if (this.result.type.isDouble()) {
                    return "sub rsp, 8\nmovsd [rsp], xmm0";
                }
                return "push rax";
            }
            case POP -> {
                if (this.result.type.isFloat()) {
                    return "movss xmm0, [rsp]\nadd rsp, 4";
                }
                if (this.result.type.isDouble()) {
                    return "movsd xmm0, [rsp]\nadd rsp, 8";
                }
                if (this.result.type.isByte()) {
                    return "pop rax\nmovsx rax, al";
                }
                if (this.result.type.isShort()) {
                    return "pop rax\nmovsx rax, ax";
                }
                return "pop rax";
            }
            case MOV_REG_CA -> {
                StringPair movePair = getMovOpAndRegisterFromType(operand1.type, 0);
                String register2 = getRegisterFromType(operand1.type, 1);
                String out = String.format("%s %s, %s", movePair.move(), register2, movePair.register());
                if (operand1.type.isByte()) {
                    return "movsx rcx, al";
                }
                if (operand1.type.isShort()) {
                    return "movsx rcx, ax";
                }
                return out;
            }
            case PUSH_STRUCT -> {return stack.pushStruct(operand1);}
            case MOVE_STRUCT -> {return stack.moveStruct(operand1);}
            case MOV_XMM0 -> {
                if(operand1.type.isFloat()){
                   return "cvtss2sd xmm0, xmm0";
                }
                return "nop ; mov_xmm0";
            }
            case MOV_XMM1 -> {
                if(operand1.type.isDouble()){
                    return "movsd xmm1, xmm0";
                }else{
                    return "cvtss2sd xmm1, xmm0";
                }
            }
            case MOV_XMM2 -> {
                if(operand1.type.isDouble()){
                    return "movsd xmm2, xmm0";
                }else{
                    return "cvtss2sd xmm2, xmm0";
                }
            }
            case MOV_XMM3 -> {
                if(operand1.type.isDouble()){
                    return "movsd xmm3, xmm0";
                }else{
                    return "cvtss2sd xmm3, xmm0";
                }
            }
            case MOV_XMM4 -> {
                if(operand1.type.isDouble()){
                    return "movsd xmm4, xmm0";
                }else{
                    return "cvtss2sd xmm4, xmm0";
                }
            }
            case MOV_XMM5 -> {
                if(operand1.type.isDouble()){
                    return "movsd xmm5, xmm0";
                }else{
                    return "cvtss2sd xmm5, xmm0";
                }
            }
            case MOV_RDI -> {
                if(operand1.type.isByte()){
                    return "movsx rdi, al";
                }
                if(operand1.type.isShort()){
                    return "movsx rdi, ax";
                }
                if(operand1.type.isInteger()){
                    return "movsx rdi, eax";
                }
                return "mov rdi, rax";
            }
            case MOV_RSI -> {
                if(operand1.type.isByte()){
                    return "movsx rsi, al";
                }
                if(operand1.type.isShort()){
                    return "movsx rsi, ax";
                }
                if(operand1.type.isInteger()){
                    return "movsx rsi, eax";
                }
                return "mov rsi, rax";

            }
            case MOV_RCX -> {
                if(operand1.type.isByte()){
                    return "movsx rcx, al";
                }
                if(operand1.type.isShort()){
                    return "movsx rcx, ax";
                }
                if(operand1.type.isInteger()){
                    return "movsx rcx, eax";
                }
                return "mov rcx, rax";
            }
            case MOV_RDX -> {
                if(operand1.type.isByte()){
                    return "movsx rdx, al";
                }
                if(operand1.type.isShort()){
                    return "movsx rdx, ax";
                }
                if(operand1.type.isInteger()){
                    return "movsx rdx, eax";
                }
                return "mov rdx, rax";
            }
            case MOV_R8 -> {
                if(operand1.type.isByte()){
                    return "movsx r8, al";
                }
                if(operand1.type.isShort()){
                    return "movsx r8, ax";
                }
                if(operand1.type.isInteger()){
                    return "movsx r8, eax";
                }
                return "mov r8, rax";
            }
            case MOV_R9 -> {
                if(operand1.type.isByte()){
                    return "movsx r9, al";
                }
                if(operand1.type.isShort()){
                    return "movsx r9, ax";
                }
                if(operand1.type.isInteger()){
                    return "movsx r9, eax";
                }
                return "mov r9, rax";
            }
            case CALL -> {
                int stackAligment = getFunctionArgumentsStackSize(operand1.name, functions, stack);
                if (stackAligment != 0) {
                    stackAligment += Compiler.getStackPadding(stackAligment);
                    return String.format("call %s\nadd rsp, %d", operand1.name, stackAligment);
                }
                return String.format("call %s", operand1.name);
            }
            case LOGICAL_NOT -> {return "xor rax, 1";}
            case NEGATE -> {
                if(operand1.type.isByte()){
                    return "not al\ninc al";
                }
                if(operand1.type.isShort()){
                    return "not ax\ninc ax";
                }
                if(operand1.type.isInteger()){
                    return "not eax\ninc eax";
                }
                return "not rax\ninc rax";
            }
            case ALLOCATE -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                return String.format("sub rsp, %s", immediateSymbol.getValue());
            }
            case MOVE_ARG -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                return stack.moveArg(operand1, Integer.parseInt(immediateSymbol.getValue()));
            }
            case RET -> {return "mov rsp, rbp\npop rbp\nret";}
        }
        throw new CompileException(String.format("Don't know how to do %s", op));
    }
}
