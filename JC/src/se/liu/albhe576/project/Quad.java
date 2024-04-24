package se.liu.albhe576.project;

import java.util.Map;

public record Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result) {

    @Override
    public String toString() {
        return op.name() + "\t\t" +
                operand1 + "\t\t" +
                operand2 + "\t\t" +
                result;
    }


    private static int getStackAlignment(String name, Map<String, Function> functions, Stack stack) {
        int argSize = 0;
        Function function = functions.get(name);
        if (function.external) {
            return 0;
        }
        if (function.getArguments() != null) {
            Map<String, Struct> structs = stack.getStructs();
            for (StructField field : function.getArguments()) {
                argSize += SymbolTable.getStructSize(structs, field.type());
            }
        }

        return argSize;

    }

    public static String getRegisterFromType(DataType type, int registerIndex) {
        final String[] floatRegisters = new String[]{"xmm0", "xmm1"};
        final String[] generalRegisters = new String[]{"rax", "rcx"};
        final String[] byteRegisters = new String[]{"al", "cl"};
        if (type.isFloatingPoint()) {
            return floatRegisters[registerIndex];
        } else if (type.isByte()) {
            return byteRegisters[registerIndex];
        }
        return generalRegisters[registerIndex];
    }

    public static String getMovOpFromType(DataType type) {return type.isFloatingPoint() ? "movsd" : "mov";}

    public String emit(Stack stack, Map<String, Function> functions, Map<String, Constant> constants) throws CompileException {
        switch (this.op) {
            case LOAD_IMM -> {
                ImmediateSymbol imm = (ImmediateSymbol) this.operand1;
                String register = getRegisterFromType(operand1.type, 0);
                String immValue = imm.getValue();



                switch (imm.type.type) {
                    case INT -> {return String.format("mov %s, %s", register, immValue);}
                    case STRING -> {
                        return String.format("mov %s, %s", register, constants.get(immValue).label());
                    }
                    case FLOAT -> {
                        if (constants.containsKey(immValue)) {
                            return String.format("movsd %s,[%s]", register, constants.get(immValue).label());
                        }
                        throw new CompileException(String.format("Couldn't find constant '%s'", immValue));
                    }
                }
                throw new CompileException(String.format("Can't load this type? %s", imm.type.type));
            }
            case INC -> {
                if (operand1.type.isFloatingPoint()) {
                    return "mov rcx, 1\ncvtsi2sd xmm1, rcx\naddsd xmm0, xmm1";
                }
                return "inc rax";
            }
            case DEC -> {
                if (operand1.type.isFloatingPoint()) {
                    return "mov rcx, 1\ncvtsi2sd xmm1, rcx\nsubsd xmm0, xmm1";
                }
                return "dec rax";
            }
            case ADD -> {
                if (result.type.isFloatingPoint()) {
                    return "addsd xmm0, xmm1";
                }
                return "add rax, rcx";
            }
            case SUB -> {
                if (result.type.isFloatingPoint()) {
                    return "subsd xmm0, xmm1";
                }
                return "sub rax, rcx";
            }
            case MUL -> {
                if (result.type.isFloatingPoint()) {
                    return "mulsd xmm0, xmm1";
                }
                return "mul rcx";
            }
            case IMUL -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) this.operand1;
                return String.format("imul rax, %s", immediateSymbol.getValue());
            }
            case CONVERT_FLOAT_TO_INT -> {return "cvttsd2si rax, xmm0";}
            case CONVERT_INT_TO_FLOAT -> {return "cvtsi2sd xmm0, rax";}
            case CONVERT_BYTE_TO_INT -> {return "movzx rax, al";}
            case DIV -> {
                if (result.type.isFloatingPoint()) {
                    return "divsd xmm0, xmm1";
                }
                return "xor rdx, rdx\nidiv rcx";
            }
            case MOD -> {return "cdq\nxor rdx, rdx\nidiv rcx\nmov rax, rdx\n";}
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
                // nop
                if (result.type.isStruct() && operand2.type.isPointer()) {
                    return "lea rax, [rax]";
                }
                String movOp = getMovOpFromType(result.type);
                String register = getRegisterFromType(result.type, 0);
                return String.format("%s %s, [rax]", movOp, register);
            }
            case LOAD -> {
                VariableSymbol variableSymbol = (VariableSymbol) operand1;
                return stack.loadVariable(variableSymbol.id);
            }
            case SET_FIELD -> {return stack.storeField(operand2.type, operand1);}
            case GET_FIELD -> {return stack.loadField(operand1.type, operand2.name);}
            case STORE -> {
                VariableSymbol variableSymbol = (VariableSymbol) operand1;
                if (result.type.isByte()) {
                    return "movzx rax, al\n" + stack.storeVariable(variableSymbol.id);
                }
                return stack.storeVariable(variableSymbol.id);
            }
            case STORE_INDEX -> {
                String movOp = getMovOpFromType(operand1.type);
                String register = getRegisterFromType(operand1.type, 0);
                return String.format("%s [rcx], %s", movOp, register);
            }
            case CMP -> {
                if (operand1 != null && operand1.type.isFloatingPoint()) {
                    return "comisd xmm0, xmm1";
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
            case SETLE -> {return "setle al\nmovzx rax, al";}
            case SETG -> {return "setg al\nmovzx rax, al";}
            case SETGE -> {return "setge al\nmovzx rax, al";}
            case SETL -> {return "setl al\nmovzx rax, al";}
            case SETE -> {return "sete al\nmovzx rax, al";}
            case SETA -> {return "seta al\nmovzx rax, al";}
            case SETNE -> {return "setne al\nmovzx rax, al";}
            case SETAE -> {return "setae al\nmovzx rax, al";}
            case SETB -> {return "setb al\nmovzx rax, al";}
            case SETBE -> {return "setbe al\nmovzx rax, al";}
            case SHL -> {return "shl rax, cl";}
            case AND -> {return "and rax, rcx";}
            case OR -> {return "or rax, rcx";}
            case XOR -> {return "xor rax, rcx";}
            case SHR -> {return "shr rax, cl";}
            case PUSH -> {
                if (operand1.type.isFloatingPoint()) {
                    return "sub rsp, 8\nmovsd [rsp], xmm0";
                }
                return "push rax";
            }
            case POP -> {
                if (this.result.type.isFloatingPoint()) {
                    return "movsd xmm0, [rsp]\nadd rsp, 8";
                }
                if (this.result.type.isByte()) {
                    return "pop rax\nmovzx rax, al";
                }
                return "pop rax";
            }
            case MOV_RCX -> {
                if (operand1.type.isByte()) {
                    return "movzx rax, al\nmov rcx, rax";
                }
                return "mov rcx, rax";
            }
            case MOV_REG_CA -> {
                String movOp = getMovOpFromType(operand1.type);
                String register1 = getRegisterFromType(operand1.type, 0);
                String register2 = getRegisterFromType(operand1.type, 1);
                String out = String.format("%s %s, %s", movOp, register2, register1);
                if (operand1.type.isByte()) {
                    out += "\nmovzx rcx, cl";
                }
                return out;
            }
            case PUSH_STRUCT -> {return stack.pushStruct(operand1);}
            case MOVE_STRUCT -> {return stack.moveStruct(operand1);}
            case MOV_RDI -> {
                if (operand1.type.isByte()) {
                    return "movzx rax, al\nmov rdi, rax";
                }
                return "mov rdi, rax";
            }
            case MOV_XMM0 -> {return "nop ; mov_xmm0";}
            case MOV_XMM1 -> {return "movsd xmm1, xmm0";}
            case MOV_XMM2 -> {return "movsd xmm2, xmm0";}
            case MOV_XMM3 -> {return "movsd xmm3, xmm0";}
            case MOV_XMM4 -> {return "movsd xmm4, xmm0";}
            case MOV_XMM5 -> {return "movsd xmm5, xmm0";}
            case MOV_RSI -> {
                if (operand1.type.isByte()) {
                    return "movzx rax, al\nmov rsi, rax";
                }
                return "mov rsi, rax";
            }
            case MOV_RDX -> {
                if (operand1.type.isByte()) {
                    return "movzx rax, al\nmov rdx, rax";
                }
                return "mov rdx, rax";
            }
            case MOV_R8 -> {
                if (operand1.type.isByte()) {
                    return "movzx rax, al\nmov r8, rax";
                }
                return "mov r8, rax";
            }
            case MOV_R9 -> {
                if (operand1.type.isByte()) {
                    return "movzx rax, al\nmov r9, rax";
                }
                return "mov r9, rax";
            }
            case CALL -> {
                int stackAligment = getStackAlignment(operand1.name, functions, stack);

                if (stackAligment != 0) {
                    if (stackAligment % 16 != 0) {
                        stackAligment += 16 - (stackAligment % 16);
                    }
                    return String.format("call %s\nadd rsp, %d", operand1.name, stackAligment);
                }
                return String.format("call %s", operand1.name);
            }
            case LOGICAL_NOT -> {return "xor rax, 1";}
            case NEGATE -> {return "not rax\ninc rax";}
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
