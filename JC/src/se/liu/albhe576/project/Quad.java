package se.liu.albhe576.project;

import java.util.Map;

public record Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result) {

    private static final Register[] FLOAT_REGISTERS = new Register[]{Register.XMM0, Register.XMM1, Register.XMM2};
    private static final Register[] INTEGER_REGISTERS = new Register[]{Register.EAX, Register.ECX, Register.EBX};
    private static final Register[] BYTE_REGISTER = new Register[]{Register.AL, Register.CL, Register.BL};
    private static final Register[] SHORT_REGISTER = new Register[]{Register.AX, Register.CX, Register.BX};
    private static final Register[] LONG_REGISTERS = new Register[]{Register.RAX, Register.RCX, Register.RBX};

    public static Register getRegisterFromType(DataType type, int registerIndex) {
        if (type.isFloat() || type.isDouble()) {
            return FLOAT_REGISTERS[registerIndex];
        } else if (type.isByte()) {
            return BYTE_REGISTER[registerIndex];
        } else if (type.isShort()) {
            return SHORT_REGISTER[registerIndex];
        } else if (type.isInt()) {
            return INTEGER_REGISTERS[registerIndex];
        }
        return LONG_REGISTERS[registerIndex];
    }

    public static Operation getMovOpFromType(DataType type) {
        if(type.isFloat()){
            return Operation.MOVSS;
        }
        return type.isDouble() ? Operation.MOVSD : Operation.MOV;
    }
    public static Operand getConstantStringLocation(Map<String, Constant> constants, DataType type, String label){
        if(type.isFloat()){
            return new Operand(new Address(constants.get(label).label(), true));
        }else if(type.type == DataTypes.STRING){
            return new Operand(new Address(constants.get(label).label(), false));
        }
        return new Operand(label);
    }
    private Instruction[] loadImmediate(Map<String, Constant> constants){
        ImmediateSymbol imm = (ImmediateSymbol) this.operand1;
        Operand immediate = getConstantStringLocation(constants, imm.type, imm.getValue());
        Operand target = new Operand(new Address(getRegisterFromType(imm.type, 0)));
        Operation op = getMovOpFromType(imm.type);

        return new Instruction[]{new Instruction(op, target, immediate)};
    }
    public Instruction[] moveStruct(Map<String, Struct> structs, Symbol value){
        Struct valueStruct  = structs.get(value.type.name);
        return new Instruction[]{
                new Instruction(Operation.MOV, Register.RSI, Register.RAX),
                new Instruction(Operation.MOV, Register.RDI, Register.RCX),
                new Instruction(Operation.MOV, Register.RCX, new Operand(String.valueOf(valueStruct.getSize(structs)))),
                new Instruction(Operation.REP, Register.MOVSB)
        };
    }
    public Instruction getSignExtend(Register target, DataType value){return new Instruction(Operation.MOVSX, target, getRegisterFromType(value, 0));}

    public Instruction[] emitInstruction(Map<String, Function> functions, Map<String, Constant> constants, Map<String, Struct> structs) throws CompileException {
        switch (this.op) {
            case LOAD_IMM -> {return this.loadImmediate(constants);}
            case INC -> {
                if (operand1.type.isFloat()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOV, Register.RCX, new Operand("1")),
                            new Instruction(Operation.CVTSI2SS, Register.XMM1, Register.RCX),
                            new Instruction(Operation.ADDSS, Register.XMM0, Register.XMM1),
                    };
                }
                if (operand1.type.isDouble()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOV, Register.RCX, new Operand("1")),
                            new Instruction(Operation.CVTSI2SD, Register.XMM1, Register.RCX),
                            new Instruction(Operation.ADDSD, Register.XMM0, Register.XMM1),
                    };
                }
                return new Instruction[]{new Instruction(Operation.INC, Register.RAX)};
            }
            case DEC -> {
                if (operand1.type.isFloat()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOV, Register.RCX, new Operand("1")),
                            new Instruction(Operation.CVTSI2SS, Register.XMM1, Register.RCX),
                            new Instruction(Operation.SUBSS, Register.XMM0, Register.XMM1),
                    };
                }
                if (operand1.type.isDouble()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOV, Register.RCX, new Operand("1")),
                            new Instruction(Operation.CVTSI2SD, Register.XMM1, Register.RCX),
                            new Instruction(Operation.SUBSD, Register.XMM0, Register.XMM1),
                    };
                }
                return new Instruction[]{new Instruction(Operation.DEC, Register.RAX)};
            }
            case IMUL -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) this.operand1;
                return new Instruction[]{new Instruction(Operation.IMUL, Register.RAX, new Operand(immediateSymbol.getValue()))};
            }
            case ADD -> {
                Register reg0 = getRegisterFromType(result.type, 0);
                Register reg1 = getRegisterFromType(result.type, 1);
                Operation operation;
                if (result.type.isDouble()) {
                    operation = Operation.ADDSD;
                }
                else{
                    operation = result.type.isFloat() ? Operation.ADDSS  : Operation.ADD;
                }
                return new Instruction[]{new Instruction(operation, reg0, reg1)};
            }
            case SUB -> {
                Register reg0 = getRegisterFromType(result.type, 0);
                Register reg1 = getRegisterFromType(result.type, 1);
                Operation operation;
                if (result.type.isDouble()) {
                    operation = Operation.SUBSD;
                }
                else{
                    operation = result.type.isFloat() ? Operation.SUBSS  : Operation.SUB;
                }
                return new Instruction[]{new Instruction(operation, reg0, reg1)};
            }
            case MUL -> {
                Register reg0 = getRegisterFromType(result.type, 0);
                Register reg1 = getRegisterFromType(result.type, 1);
                Operation operation;
                if (result.type.isDouble() || result.type.isFloat()) {
                    operation = result.type.isFloat() ? Operation.MULSS : Operation.MULSD;
                    return new Instruction[]{new Instruction(operation, reg0, reg1)};
                }
                return new Instruction[]{new Instruction(Operation.MUL, reg1)};
            }
            case DIV -> {
                Register reg0 = getRegisterFromType(result.type, 0);
                Register reg1 = getRegisterFromType(result.type, 1);
                if (result.type.isDouble()) {
                    return new Instruction[]{new Instruction(Operation.DIVSD, reg0, reg1)};
                }
                if (result.type.isFloat()) {
                    return new Instruction[]{new Instruction(Operation.DIVSS, reg0, reg1)};
                }
                return new Instruction[]{
                        new Instruction(Operation.XOR, Register.RDX, Register.RDX),
                        new Instruction(Operation.IDIV, reg1),
                };
            }
            case MOD -> {
                return new Instruction[]{
                        new Instruction(Operation.XOR, Register.RDX, Register.RDX),
                        new Instruction(Operation.IDIV, Register.RCX),
                        new Instruction(Operation.MOV, Register.RAX, Register.RDX)
                };
            }
            case ADDI, SUBI-> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                Operation operation = op == QuadOp.ADDI ? Operation.ADD : Operation.SUB;
                return new Instruction[]{
                        new Instruction(operation, Register.RAX, new Operand(immediateSymbol.getValue()))
                };
            }
            case LOAD_VARIABLE_POINTER -> {
                VariableSymbol variableSymbol = (VariableSymbol) operand1;
                return new Instruction[]{
                        new Instruction(Operation.LEA, Register.RAX, new Operand(new Address(Register.RBP, variableSymbol.offset)))
                };
            }
            case LOAD_FIELD_POINTER -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                return new Instruction[]{
                        new Instruction(Operation.LEA, Register.RAX, new Operand(new Address(Register.RAX, Integer.parseInt(immediateSymbol.getValue()))))
                };
            }
            case LOAD -> {
                Operation operation = getMovOpFromType(result.type);
                Register target = getRegisterFromType(result.type, 0);
                Operand value = new Operand(new Address(Register.RAX, true));

                if(operand2 != null) {
                    ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                    value.address.effective = true;
                    value.address.offset = Integer.parseInt(immediateSymbol.getValue());
                }
                Instruction[] out = new Instruction[]{
                        new Instruction(operation, target, value)
                };
                if(operand1.type.isByte() || operand1.type.isShort()){
                    return new Instruction[]{
                            out[0],
                           getSignExtend(Register.RAX, operand1.type)
                    };
                }
                return out;
            }
            case STORE -> {
                return new Instruction[]{
                        new Instruction(
                            getMovOpFromType(operand1.type),
                            new Operand(new Address(Register.RCX, true)),
                            getRegisterFromType(operand1.type, 0)
                        )
                };
            }
            case CMP -> {
                if (operand1.type.isDouble()) {
                    return new Instruction[]{
                            new Instruction(Operation.COMISD, Register.XMM0, Register.XMM1)
                    };
                }
                if (operand1.type.isFloat()) {
                    return new Instruction[]{
                            new Instruction(Operation.COMISS, Register.XMM0, Register.XMM1)
                    };
                }
                return new Instruction[]{
                        new Instruction(
                            Operation.CMP,
                            getRegisterFromType(operand1.type, 0),
                            getRegisterFromType(operand1.type, 1)
                        )
                };
            }
            case JMP -> {return new Instruction[]{new Instruction(Operation.JMP, new Operand(operand1.name))};}
            case JNZ -> {return new Instruction[]{new Instruction(Operation.JNZ, new Operand(operand1.name))};}
            case JE -> {return new Instruction[]{new Instruction(Operation.JE, new Operand(operand1.name))};}
            case JL -> {return new Instruction[]{new Instruction(Operation.JL, new Operand(operand1.name))};}
            case JLE -> {return new Instruction[]{new Instruction(Operation.JLE, new Operand(operand1.name))};}
            case JG -> {return new Instruction[]{new Instruction(Operation.JG, new Operand(operand1.name))};}
            case JGE -> {return new Instruction[]{new Instruction(Operation.JGE, new Operand(operand1.name))};}
            case JA -> {return new Instruction[]{new Instruction(Operation.JA, new Operand(operand1.name))};}
            case JAE -> {return new Instruction[]{new Instruction(Operation.JAE, new Operand(operand1.name))};}
            case JB -> {return new Instruction[]{new Instruction(Operation.JB, new Operand(operand1.name))};}
            case JBE -> {return new Instruction[]{new Instruction(Operation.JBE, new Operand(operand1.name))};}
            case JNE -> {return new Instruction[]{new Instruction(Operation.JNE, new Operand(operand1.name))};}
            case LABEL -> {return new Instruction[]{new Instruction(operand1.name)};}
            case SETLE -> {return new Instruction[]{
                    new Instruction(Operation.SETLE, Register.AL),
                    new Instruction(Operation.MOVSX, Register.RAX, Register.AL)
            };}
            case SETG -> {return new Instruction[]{
                    new Instruction(Operation.SETG, Register.AL),
                    new Instruction(Operation.MOVSX, Register.RAX, Register.AL)
            };}
            case SETGE -> {
                    return new Instruction[]{
                    new Instruction(Operation.SETGE, Register.AL),
                    new Instruction(Operation.MOVSX, Register.RAX, Register.AL)
            };
            }
            case SETL -> {return new Instruction[]{
                    new Instruction(Operation.SETL, Register.AL),
                    new Instruction(Operation.MOVSX, Register.RAX, Register.AL)
            };}
            case SETE -> {return new Instruction[]{
                    new Instruction(Operation.SETE, Register.AL),
                    new Instruction(Operation.MOVSX, Register.RAX, Register.AL)
            };}
            case SETA -> {return new Instruction[]{
                    new Instruction(Operation.SETA, Register.AL),
                    new Instruction(Operation.MOVSX, Register.RAX, Register.AL)
            };}
            case SETNE -> {return new Instruction[]{
                    new Instruction(Operation.SETNE, Register.AL),
                    new Instruction(Operation.MOVSX, Register.RAX, Register.AL)
            };}
            case SETAE -> {return new Instruction[]{
                    new Instruction(Operation.SETAE, Register.AL),
                    new Instruction(Operation.MOVSX, Register.RAX, Register.AL)
            };}
            case SETB -> {return new Instruction[]{
                    new Instruction(Operation.SETB, Register.AL),
                    new Instruction(Operation.MOVSX, Register.RAX, Register.AL)
            };}
            case SETBE -> {return new Instruction[]{
                    new Instruction(Operation.SETBE, Register.AL),
                    new Instruction(Operation.MOVSX, Register.RAX, Register.AL)
            };}
            case CONVERT_DOUBLE_TO_FLOAT-> {
                return new Instruction[]{
                        new Instruction(Operation.CVTSD2SS, Register.XMM0, Register.XMM0)
                };
            }
            case CONVERT_DOUBLE_TO_LONG-> {return new Instruction[]{
                    new Instruction(Operation.CVTTSD2SI, Register.RAX, Register.XMM0)
            };}
            case CONVERT_FLOAT_TO_DOUBLE -> {
                return new Instruction[]{
                        new Instruction(Operation.CVTSS2SD, Register.XMM0, Register.XMM0)
                };
            }
            case CONVERT_FLOAT_TO_INT-> {return new Instruction[]{
                    new Instruction(Operation.CVTSS2SI, Register.EAX, Register.XMM0)
            };}
            case CONVERT_INT_TO_FLOAT -> {return new Instruction[]{
                    new Instruction(Operation.CVTSI2SS, Register.XMM0, Register.EAX)
            };
            }
            case CONVERT_LONG_TO_DOUBLE -> {
                return new Instruction[]{
                        new Instruction(Operation.CVTSI2SD, Register.XMM0, Register.RAX)
                };
            }
            case ZX_SHORT -> {return new Instruction[]{new Instruction(Operation.MOVSX, Register.RAX, Register.AX)};}
            case ZX_INT-> {return new Instruction[]{new Instruction(Operation.MOVSX, Register.RAX, Register.EAX)};}
            case ZX_BYTE-> {return new Instruction[]{new Instruction(Operation.MOVSX, Register.RAX, Register.AL)};}
            case SHL -> {return new Instruction[]{new Instruction(Operation.SHL, Register.RAX, Register.CL)};}
            case AND -> {return new Instruction[]{new Instruction(Operation.AND, Register.RAX, Register.RCX)};}
            case OR -> {return new Instruction[]{new Instruction(Operation.OR, Register.RAX, Register.RCX)};}
            case XOR -> {return new Instruction[]{new Instruction(Operation.XOR, Register.RAX, Register.RCX)};}
            case SHR -> {return new Instruction[]{new Instruction(Operation.SHR, Register.RAX, Register.CL)};}
            case PUSH_RCX -> {return new Instruction[]{new Instruction(Operation.PUSH, Register.RCX)};}
            case POP_RCX -> {return new Instruction[]{new Instruction(Operation.POP, Register.RCX)};}
            case PUSH -> {
                if (this.result.type.isFloat()) {
                    return new Instruction[]{
                            new Instruction(Operation.SUB, Register.RSP, new Operand("16")),
                            new Instruction(Operation.MOVSS, new Operand(new Address(Register.RSP, true)), Register.XMM0),
                    };
                }
                if (this.result.type.isDouble()) {
                    return new Instruction[]{
                            new Instruction(Operation.SUB, Register.RSP, new Operand("16")),
                            new Instruction(Operation.MOVSD, new Operand(new Address(Register.RSP, true)), Register.XMM0),
                    };
                }
                return new Instruction[]{
                        new Instruction(Operation.SUB, Register.RSP, new Operand("8")),
                        new Instruction(Operation.PUSH, Register.RAX),
                };
            }
            case POP -> {
                if (this.result.type.isFloat()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOVSS, Register.XMM0, new Operand(new Address(Register.RSP, true))),
                            new Instruction(Operation.ADD, Register.RSP, new Operand("16")),
                    };
                }
                if (this.result.type.isDouble()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOVSD, Register.XMM0, new Operand(new Address(Register.RSP, true))),
                            new Instruction(Operation.ADD, Register.RSP, new Operand("16")),
                    };
                }
                if (this.result.type.isByte() || result.type.isShort()) {
                    return new Instruction[]{
                            new Instruction(Operation.POP, Register.RAX),
                            new Instruction(Operation.ADD, Register.RSP, new Operand("8")),
                            getSignExtend(Register.RAX, this.result.type)
                    };
                }
                return new Instruction[]{
                        new Instruction(Operation.POP, Register.RAX),
                        new Instruction(Operation.ADD, Register.RSP, new Operand("8")),
                };
            }
            case MOV_REG_CA -> {
                if (operand1.type.isByte() || operand1.type.isShort()) {
                    return new Instruction[]{getSignExtend(Register.RCX, operand1.type)};
                }
                return new Instruction[]{
                        new Instruction(getMovOpFromType(operand1.type), getRegisterFromType(operand1.type, 1), getRegisterFromType(operand1.type, 0))
                };
            }
            case MOVE_STRUCT -> {return moveStruct(structs, operand1);}
            case MOV_XMM0 -> {
                if(operand1.type.isFloat()){
                    return new Instruction[]{
                            new Instruction(Operation.CVTSS2SD, Register.XMM0, Register.XMM0)
                    };
                }
                return new Instruction[0];
            }
            case MOV_XMM1 -> {
                Operation op = operand1.type.isDouble() ? Operation.MOVSD : Operation.CVTSS2SD;
                return new Instruction[]{new Instruction(op, Register.XMM1, Register.XMM0)};
            }
            case MOV_XMM2 -> {
                Operation op = operand1.type.isDouble() ? Operation.MOVSD : Operation.CVTSS2SD;
                return new Instruction[]{new Instruction(op, Register.XMM2, Register.XMM0)};
            }
            case MOV_XMM3 -> {
                Operation op = operand1.type.isDouble() ? Operation.MOVSD : Operation.CVTSS2SD;
                return new Instruction[]{new Instruction(op, Register.XMM3, Register.XMM0)};
            }
            case MOV_XMM4 -> {
                Operation op = operand1.type.isDouble() ? Operation.MOVSD : Operation.CVTSS2SD;
                return new Instruction[]{new Instruction(op, Register.XMM4, Register.XMM0)};
            }
            case MOV_XMM5 -> {
                Operation op = operand1.type.isDouble() ? Operation.MOVSD : Operation.CVTSS2SD;
                return new Instruction[]{new Instruction(op, Register.XMM5, Register.XMM0)};
            }
            case MOV_RDI -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, Register.RDI, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, Register.RDI, Register.RAX)};
            }
            case MOV_RSI -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, Register.RSI, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, Register.RSI, Register.RAX)};

            }
            case MOV_RCX -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, Register.RCX, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, Register.RCX, Register.RAX)};
            }
            case MOV_RDX -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, Register.RDX, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, Register.RDX, Register.RAX)};
            }
            case MOV_R8 -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, Register.R8, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, Register.R8, Register.RAX)};
            }
            case MOV_R9 -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, Register.R9, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, Register.R9, Register.RAX)};
            }
            case CALL -> {
                int stackAligment = Struct.getFunctionArgumentsStackSize(operand1.name, functions, structs);
                if (stackAligment != 0) {
                    stackAligment += Compiler.getStackPadding(stackAligment);
                    return new Instruction[]{
                            new Instruction(Operation.CALL, new Operand(operand1.name)),
                            new Instruction(Operation.ADD, Register.RSP, new Operand(String.valueOf(stackAligment))),
                    };
                }
                return new Instruction[]{new Instruction(Operation.CALL, new Operand(operand1.name))};
            }
            case LOGICAL_NOT -> {
                return new Instruction[]{new Instruction(Operation.XOR, Register.RAX, new Operand("1"))};
            }
            case NEGATE -> {
                Register reg = getRegisterFromType(operand1.type, 0);
                return new Instruction[]{
                        new Instruction(Operation.NOT, reg),
                        new Instruction(Operation.INC, reg),
                };
            }
            case ALLOCATE -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                return new Instruction[]{new Instruction(Operation.SUB, Register.RSP, new Operand(immediateSymbol.getValue()))};
            }
            case MOVE_ARG -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                int offset = Integer.parseInt(immediateSymbol.getValue());
                if(operand1.type.isStruct()){

                    Instruction lea  = new Instruction(Operation.LEA, Register.RCX, new Operand(new Address(Register.RSP, offset)));
                    Instruction [] movedStruct = moveStruct(structs, operand1);
                    Instruction[] out = new Instruction[1 + movedStruct.length];
                    out[0] = lea;
                    System.arraycopy(movedStruct, 0, out, 1, out.length - 1);
                    return out;
                }

                return new Instruction[]{
                        new Instruction(
                                getMovOpFromType(operand1.type),
                                new Operand(new Address(Register.RSP, offset)),
                                getRegisterFromType(operand1.type, 0)
                        )
                };
            }
            case RET -> {
                return new Instruction[]{
                        new Instruction(Operation.MOV, Register.RSP, Register.RBP),
                        new Instruction(Operation.POP, Register.RBP),
                        new Instruction(Operation.RET),
                };
            }
        }
        throw new CompileException(String.format("Don't know how to do %s", op));
    }
}
