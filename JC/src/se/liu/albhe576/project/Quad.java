package se.liu.albhe576.project;

import java.util.Map;

public record Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result) {

    private static final RegisterType[] FLOAT_REGISTER_TYPES = new RegisterType[]{RegisterType.XMM0, RegisterType.XMM1, RegisterType.XMM2};
    private static final RegisterType[] INTEGER_REGISTER_TYPES = new RegisterType[]{RegisterType.EAX, RegisterType.ECX, RegisterType.EBX, RegisterType.EDX};
    private static final RegisterType[] BYTE_REGISTER_TYPE = new RegisterType[]{RegisterType.AL, RegisterType.CL, RegisterType.BL, RegisterType.DL};
    private static final RegisterType[] SHORT_REGISTER_TYPE = new RegisterType[]{RegisterType.AX, RegisterType.CX, RegisterType.BX, RegisterType.DX};
    private static final RegisterType[] LONG_REGISTER_TYPES = new RegisterType[]{RegisterType.RAX, RegisterType.RCX, RegisterType.RBX, RegisterType.RDX};

    public static RegisterType getRegisterFromType(DataType type, int registerIndex) {
        if (type.isFloat() || type.isDouble()) {
            return FLOAT_REGISTER_TYPES[registerIndex];
        } else if (type.isByte()) {
            return BYTE_REGISTER_TYPE[registerIndex];
        } else if (type.isShort()) {
            return SHORT_REGISTER_TYPE[registerIndex];
        } else if (type.isInt()) {
            return INTEGER_REGISTER_TYPES[registerIndex];
        }
        return LONG_REGISTER_TYPES[registerIndex];
    }

    public static Operation getMovOpFromType(DataType type) {
        if(type.isFloat()){
            return Operation.MOVSS;
        }
        return type.isDouble() ? Operation.MOVSD : Operation.MOV;
    }
    public static Operand getConstantStringLocation(Map<String, Constant> constants, DataType type, String label){
        if(type.isFloat()){
            return new Label(constants.get(label).label(), true);
        }else if(type.type == DataTypes.STRING){
            return new Label(constants.get(label).label(), false);
        }
        return new Immediate(label);
    }
    private Instruction[] loadImmediate(Map<String, Constant> constants){
        ImmediateSymbol imm = (ImmediateSymbol) this.operand1;
        Operand immediate = getConstantStringLocation(constants, imm.type, imm.getValue());
        Operand target = new Register(getRegisterFromType(imm.type, 0));
        Operation op = getMovOpFromType(imm.type);

        return new Instruction[]{new Instruction(op, target, immediate)};
    }
    public Instruction[] moveStruct(Map<String, Struct> structs, Symbol value){
        Struct valueStruct  = structs.get(value.type.name);
        return new Instruction[]{
                new Instruction(Operation.MOV, RegisterType.RSI, RegisterType.RAX),
                new Instruction(Operation.MOV, RegisterType.RDI, RegisterType.RCX),
                new Instruction(Operation.MOV, RegisterType.RCX, new Immediate(String.valueOf(valueStruct.getSize(structs)))),
                new Instruction(Operation.REP, RegisterType.MOVSB)
        };
    }
    public Instruction getSignExtend(RegisterType target, DataType value){return new Instruction(Operation.MOVSX, target, getRegisterFromType(value, 0));}

    public Instruction[] emitInstruction(Map<String, Function> functions, Map<String, Constant> constants, Map<String, Struct> structs) throws CompileException {
        switch (this.op) {
            case LOAD_IMM -> {return this.loadImmediate(constants);}
            case INC -> {
                if (operand1.type.isFloat()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOV, RegisterType.RCX, new Immediate("1")),
                            new Instruction(Operation.CVTSI2SS, RegisterType.XMM1, RegisterType.RCX),
                            new Instruction(Operation.ADDSS, RegisterType.XMM0, RegisterType.XMM1),
                    };
                }
                if (operand1.type.isDouble()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOV, RegisterType.RCX, new Immediate("1")),
                            new Instruction(Operation.CVTSI2SD, RegisterType.XMM1, RegisterType.RCX),
                            new Instruction(Operation.ADDSD, RegisterType.XMM0, RegisterType.XMM1),
                    };
                }
                return new Instruction[]{new Instruction(Operation.INC, RegisterType.RAX)};
            }
            case DEC -> {
                if (operand1.type.isFloat()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOV, RegisterType.RCX, new Immediate("1")),
                            new Instruction(Operation.CVTSI2SS, RegisterType.XMM1, RegisterType.RCX),
                            new Instruction(Operation.SUBSS, RegisterType.XMM0, RegisterType.XMM1),
                    };
                }
                if (operand1.type.isDouble()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOV, RegisterType.RCX, new Immediate("1")),
                            new Instruction(Operation.CVTSI2SD, RegisterType.XMM1, RegisterType.RCX),
                            new Instruction(Operation.SUBSD, RegisterType.XMM0, RegisterType.XMM1),
                    };
                }
                return new Instruction[]{new Instruction(Operation.DEC, RegisterType.RAX)};
            }
            case IMUL -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) this.operand1;
                return new Instruction[]{new Instruction(Operation.IMUL, RegisterType.RAX, new Immediate(immediateSymbol.getValue()))};
            }
            case ADD -> {
                RegisterType reg0 = getRegisterFromType(result.type, 0);
                RegisterType reg1 = getRegisterFromType(result.type, 1);
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
                RegisterType reg0 = getRegisterFromType(result.type, 0);
                RegisterType reg1 = getRegisterFromType(result.type, 1);
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
                RegisterType reg0 = getRegisterFromType(result.type, 0);
                RegisterType reg1 = getRegisterFromType(result.type, 1);
                Operation operation;
                if (result.type.isDouble() || result.type.isFloat()) {
                    operation = result.type.isFloat() ? Operation.MULSS : Operation.MULSD;
                    return new Instruction[]{new Instruction(operation, reg0, reg1)};
                }
                return new Instruction[]{new Instruction(Operation.MUL, reg1)};
            }
            case DIV -> {
                RegisterType reg0 = getRegisterFromType(result.type, 0);
                RegisterType reg1 = getRegisterFromType(result.type, 1);
                if (result.type.isDouble()) {
                    return new Instruction[]{new Instruction(Operation.DIVSD, reg0, reg1)};
                }
                if (result.type.isFloat()) {
                    return new Instruction[]{new Instruction(Operation.DIVSS, reg0, reg1)};
                }
                return new Instruction[]{
                        new Instruction(Operation.XOR, RegisterType.RDX, RegisterType.RDX),
                        new Instruction(Operation.IDIV, reg1),
                };
            }
            case MOD -> {
                RegisterType reg0 = getRegisterFromType(result.type,  0);
                RegisterType reg1 = getRegisterFromType(result.type,  3);
                Operation moveOp = getMovOpFromType(result.type);
                return new Instruction[]{
                        new Instruction(Operation.XOR, RegisterType.RDX, RegisterType.RDX),
                        new Instruction(Operation.IDIV, RegisterType.RCX),
                        new Instruction(moveOp, reg0, reg1)
                };
            }
            case ADDI, SUBI-> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                Operation operation = op == QuadOp.ADDI ? Operation.ADD : Operation.SUB;
                return new Instruction[]{
                        new Instruction(operation, RegisterType.RAX, new Immediate(immediateSymbol.getValue()))
                };
            }
            case LOAD_VARIABLE_POINTER -> {
                VariableSymbol variableSymbol = (VariableSymbol) operand1;
                return new Instruction[]{
                        new Instruction(Operation.LEA, RegisterType.RAX, new Register(RegisterType.RBP, variableSymbol.offset))
                };
            }
            case LOAD_FIELD_POINTER -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                return new Instruction[]{
                        new Instruction(Operation.LEA, RegisterType.RAX, new Register(RegisterType.RAX, Integer.parseInt(immediateSymbol.getValue())))
                };
            }
            case LOAD -> {
                Operation operation = result.type.isStruct() ? Operation.LEA : getMovOpFromType(result.type);

                RegisterType target = getRegisterFromType(result.type, 0);
                Register value = new Register(RegisterType.RAX, true);

                if(operand2 != null) {
                    ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                    value.effective = true;
                    value.offset = Integer.parseInt(immediateSymbol.getValue());
                }
                Instruction[] out = new Instruction[]{
                        new Instruction(operation, target, value)
                };
                if(operand1.type.isByte() || operand1.type.isShort()){
                    return new Instruction[]{
                            out[0],
                           getSignExtend(RegisterType.RAX, operand1.type)
                    };
                }
                return out;
            }
            case STORE -> {
                if(operand1.type.isStruct()){
                    return moveStruct(structs, operand1);
                }

                return new Instruction[]{
                        new Instruction(
                            getMovOpFromType(operand1.type),
                            new Register(RegisterType.RCX, true),
                            getRegisterFromType(operand1.type, 0)
                        )
                };
            }
            case CMP -> {
                if (operand1.type.isDouble()) {
                    return new Instruction[]{
                            new Instruction(Operation.COMISD, RegisterType.XMM0, RegisterType.XMM1)
                    };
                }
                if (operand1.type.isFloat()) {
                    return new Instruction[]{
                            new Instruction(Operation.COMISS, RegisterType.XMM0, RegisterType.XMM1)
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
            case JMP -> {return new Instruction[]{new Instruction(Operation.JMP, new Label(operand1.name))};}
            case JNZ -> {return new Instruction[]{new Instruction(Operation.JNZ, new Label(operand1.name))};}
            case JE -> {return new Instruction[]{new Instruction(Operation.JE, new Label(operand1.name))};}
            case JL -> {return new Instruction[]{new Instruction(Operation.JL, new Label(operand1.name))};}
            case JLE -> {return new Instruction[]{new Instruction(Operation.JLE, new Label(operand1.name))};}
            case JG -> {return new Instruction[]{new Instruction(Operation.JG, new Label(operand1.name))};}
            case JGE -> {return new Instruction[]{new Instruction(Operation.JGE, new Label(operand1.name))};}
            case JA -> {return new Instruction[]{new Instruction(Operation.JA, new Label(operand1.name))};}
            case JAE -> {return new Instruction[]{new Instruction(Operation.JAE, new Label(operand1.name))};}
            case JB -> {return new Instruction[]{new Instruction(Operation.JB, new Label(operand1.name))};}
            case JBE -> {return new Instruction[]{new Instruction(Operation.JBE, new Label(operand1.name))};}
            case JNE -> {return new Instruction[]{new Instruction(Operation.JNE, new Label(operand1.name))};}
            case LABEL -> {return new Instruction[]{new Instruction(operand1.name)};}
            case SETLE -> {return new Instruction[]{
                    new Instruction(Operation.SETLE, RegisterType.AL),
                    new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)
            };}
            case SETG -> {return new Instruction[]{
                    new Instruction(Operation.SETG, RegisterType.AL),
                    new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)
            };}
            case SETGE -> {
                    return new Instruction[]{
                    new Instruction(Operation.SETGE, RegisterType.AL),
                    new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)
            };
            }
            case SETL -> {return new Instruction[]{
                    new Instruction(Operation.SETL, RegisterType.AL),
                    new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)
            };}
            case SETE -> {return new Instruction[]{
                    new Instruction(Operation.SETE, RegisterType.AL),
                    new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)
            };}
            case SETA -> {return new Instruction[]{
                    new Instruction(Operation.SETA, RegisterType.AL),
                    new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)
            };}
            case SETNE -> {return new Instruction[]{
                    new Instruction(Operation.SETNE, RegisterType.AL),
                    new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)
            };}
            case SETAE -> {return new Instruction[]{
                    new Instruction(Operation.SETAE, RegisterType.AL),
                    new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)
            };}
            case SETB -> {return new Instruction[]{
                    new Instruction(Operation.SETB, RegisterType.AL),
                    new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)
            };}
            case SETBE -> {return new Instruction[]{
                    new Instruction(Operation.SETBE, RegisterType.AL),
                    new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)
            };}
            case CONVERT_DOUBLE_TO_FLOAT-> {
                return new Instruction[]{
                        new Instruction(Operation.CVTSD2SS, RegisterType.XMM0, RegisterType.XMM0)
                };
            }
            case CONVERT_DOUBLE_TO_LONG-> {return new Instruction[]{
                    new Instruction(Operation.CVTTSD2SI, RegisterType.RAX, RegisterType.XMM0)
            };}
            case CONVERT_FLOAT_TO_DOUBLE -> {
                return new Instruction[]{
                        new Instruction(Operation.CVTSS2SD, RegisterType.XMM0, RegisterType.XMM0)
                };
            }
            case CONVERT_FLOAT_TO_INT-> {return new Instruction[]{
                    new Instruction(Operation.CVTSS2SI, RegisterType.EAX, RegisterType.XMM0)
            };}
            case CONVERT_INT_TO_FLOAT -> {return new Instruction[]{
                    new Instruction(Operation.CVTSI2SS, RegisterType.XMM0, RegisterType.EAX)
            };
            }
            case CONVERT_LONG_TO_DOUBLE -> {
                return new Instruction[]{
                        new Instruction(Operation.CVTSI2SD, RegisterType.XMM0, RegisterType.RAX)
                };
            }
            case SIGN_EXTEND_SHORT -> {return new Instruction[]{new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AX)};}
            case SIGN_EXTEND_INT -> {return new Instruction[]{new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.EAX)};}
            case SIGN_EXTEND_BYTE -> {return new Instruction[]{new Instruction(Operation.MOVSX, RegisterType.RAX, RegisterType.AL)};}
            case SHL -> {return new Instruction[]{new Instruction(Operation.SHL, RegisterType.RAX, RegisterType.CL)};}
            case AND -> {return new Instruction[]{new Instruction(Operation.AND, RegisterType.RAX, RegisterType.RCX)};}
            case OR -> {return new Instruction[]{new Instruction(Operation.OR, RegisterType.RAX, RegisterType.RCX)};}
            case XOR -> {return new Instruction[]{new Instruction(Operation.XOR, RegisterType.RAX, RegisterType.RCX)};}
            case SHR -> {return new Instruction[]{new Instruction(Operation.SHR, RegisterType.RAX, RegisterType.CL)};}
            case PUSH_RCX -> {return new Instruction[]{new Instruction(Operation.PUSH, RegisterType.RCX)};}
            case POP_RCX -> {return new Instruction[]{new Instruction(Operation.POP, RegisterType.RCX)};}
            case PUSH -> {
                if (this.result.type.isFloat()) {
                    return new Instruction[]{
                            new Instruction(Operation.SUB, RegisterType.RSP, new Immediate("16")),
                            new Instruction(Operation.MOVSS, new Register(RegisterType.RSP, true), RegisterType.XMM0),
                    };
                }
                if (this.result.type.isDouble()) {
                    return new Instruction[]{
                            new Instruction(Operation.SUB, RegisterType.RSP, new Immediate("16")),
                            new Instruction(Operation.MOVSD, new Register(RegisterType.RSP, true), RegisterType.XMM0),
                    };
                }
                return new Instruction[]{
                        new Instruction(Operation.SUB, RegisterType.RSP, new Immediate("8")),
                        new Instruction(Operation.PUSH, RegisterType.RAX),
                };
            }
            case POP -> {
                if (this.result.type.isFloat()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOVSS, RegisterType.XMM0, new Register(RegisterType.RSP, true)),
                            new Instruction(Operation.ADD, RegisterType.RSP, new Immediate("16")),
                    };
                }
                if (this.result.type.isDouble()) {
                    return new Instruction[]{
                            new Instruction(Operation.MOVSD, RegisterType.XMM0, new Register(RegisterType.RSP, true)),
                            new Instruction(Operation.ADD, RegisterType.RSP, new Immediate("16")),
                    };
                }
                if (this.result.type.isByte() || result.type.isShort()) {
                    return new Instruction[]{
                            new Instruction(Operation.POP, RegisterType.RAX),
                            new Instruction(Operation.ADD, RegisterType.RSP, new Immediate("8")),
                            getSignExtend(RegisterType.RAX, this.result.type)
                    };
                }
                return new Instruction[]{
                        new Instruction(Operation.POP, RegisterType.RAX),
                        new Instruction(Operation.ADD, RegisterType.RSP, new Immediate("8")),
                };
            }
            case MOV_REG_CA -> {
                if (operand1.type.isByte() || operand1.type.isShort()) {
                    return new Instruction[]{getSignExtend(RegisterType.RCX, operand1.type)};
                }
                return new Instruction[]{
                        new Instruction(getMovOpFromType(operand1.type), getRegisterFromType(operand1.type, 1), getRegisterFromType(operand1.type, 0))
                };
            }
            case MOVE_STRUCT -> {return moveStruct(structs, operand1);}
            case MOV_XMM0 -> {
                if(operand1.type.isFloat()){
                    return new Instruction[]{
                            new Instruction(Operation.CVTSS2SD, RegisterType.XMM0, RegisterType.XMM0)
                    };
                }
                return new Instruction[0];
            }
            case MOV_XMM1 -> {
                Operation op = operand1.type.isDouble() ? Operation.MOVSD : Operation.CVTSS2SD;
                return new Instruction[]{new Instruction(op, RegisterType.XMM1, RegisterType.XMM0)};
            }
            case MOV_XMM2 -> {
                Operation op = operand1.type.isDouble() ? Operation.MOVSD : Operation.CVTSS2SD;
                return new Instruction[]{new Instruction(op, RegisterType.XMM2, RegisterType.XMM0)};
            }
            case MOV_XMM3 -> {
                Operation op = operand1.type.isDouble() ? Operation.MOVSD : Operation.CVTSS2SD;
                return new Instruction[]{new Instruction(op, RegisterType.XMM3, RegisterType.XMM0)};
            }
            case MOV_XMM4 -> {
                Operation op = operand1.type.isDouble() ? Operation.MOVSD : Operation.CVTSS2SD;
                return new Instruction[]{new Instruction(op, RegisterType.XMM4, RegisterType.XMM0)};
            }
            case MOV_XMM5 -> {
                Operation op = operand1.type.isDouble() ? Operation.MOVSD : Operation.CVTSS2SD;
                return new Instruction[]{new Instruction(op, RegisterType.XMM5, RegisterType.XMM0)};
            }
            case MOV_RDI -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, RegisterType.RDI, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, RegisterType.RDI, RegisterType.RAX)};
            }
            case MOV_RSI -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, RegisterType.RSI, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, RegisterType.RSI, RegisterType.RAX)};

            }
            case MOV_RCX -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, RegisterType.RCX, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, RegisterType.RCX, RegisterType.RAX)};
            }
            case MOV_RDX -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, RegisterType.RDX, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, RegisterType.RDX, RegisterType.RAX)};
            }
            case MOV_R8 -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, RegisterType.R8, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, RegisterType.R8, RegisterType.RAX)};
            }
            case MOV_R9 -> {
                if(operand1.type.isByte() || operand1.type.isShort() || operand1.type.isInt()){
                    return new Instruction[]{new Instruction(Operation.MOVSX, RegisterType.R9, getRegisterFromType(operand1.type, 0))};
                }
                return new Instruction[]{new Instruction(Operation.MOV, RegisterType.R9, RegisterType.RAX)};
            }
            case CALL -> {
                int stackAligment = Struct.getFunctionArgumentsStackSize(operand1.name, functions, structs);
                if (stackAligment != 0) {
                    stackAligment += Compiler.getStackPadding(stackAligment);
                    return new Instruction[]{
                            new Instruction(Operation.CALL, new Label(operand1.name)),
                            new Instruction(Operation.ADD, RegisterType.RSP, new Immediate(String.valueOf(stackAligment))),
                    };
                }
                return new Instruction[]{new Instruction(Operation.CALL, new Label(operand1.name))};
            }
            case LOGICAL_NOT -> {
                return new Instruction[]{new Instruction(Operation.XOR, RegisterType.RAX, new Immediate("1"))};
            }
            case NEGATE -> {
                RegisterType reg = getRegisterFromType(operand1.type, 0);
                return new Instruction[]{
                        new Instruction(Operation.NOT, reg),
                        new Instruction(Operation.INC, reg),
                };
            }
            case ALLOCATE -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                return new Instruction[]{new Instruction(Operation.SUB, RegisterType.RSP, new Immediate(immediateSymbol.getValue()))};
            }
            case MOVE_ARG -> {
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                int offset = Integer.parseInt(immediateSymbol.getValue());
                if(operand1.type.isStruct()){

                    Instruction lea  = new Instruction(Operation.LEA, RegisterType.RCX, new Register(RegisterType.RSP, offset));
                    Instruction [] movedStruct = moveStruct(structs, operand1);
                    Instruction[] out = new Instruction[1 + movedStruct.length];
                    out[0] = lea;
                    for(int i = 0; i < movedStruct.length; i++){
                        out[i + 1] = movedStruct[i];
                    }
                    return out;
                }

                return new Instruction[]{
                        new Instruction(
                                getMovOpFromType(operand1.type),
                                new Register(RegisterType.RSP, offset),
                                getRegisterFromType(operand1.type, 0)
                        )
                };
            }
            case RET -> {
                return new Instruction[]{
                        new Instruction(Operation.MOV, RegisterType.RSP, RegisterType.RBP),
                        new Instruction(Operation.POP, RegisterType.RBP),
                        new Instruction(Operation.RET),
                };
            }
        }
        throw new CompileException(String.format("Don't know how to do %s", op));
    }
}
