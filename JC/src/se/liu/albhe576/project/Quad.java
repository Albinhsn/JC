package se.liu.albhe576.project;


import java.util.Map;

public record Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result) {
    @Override
    public String toString() {
        return String.format("%s %s %s %s",op.name(),operand1,operand2,result);
    }

    public static Symbol convertType(SymbolTable symbolTable, QuadList quads, Symbol symbol, DataType target){
        if(!symbol.type.isSameType(target)){
            symbol = quads.createConvert(symbolTable, symbol, target);
        }
        return symbol;
    }

    private static final Map<QuadOp, OperationType> GENERAL_QUAD_OP_TO_BINARY_OP_MAP = Map.ofEntries(
            Map.entry(QuadOp.ADD_I, OperationType.ADD),
            Map.entry(QuadOp.AND, OperationType.AND),
            Map.entry(QuadOp.OR, OperationType.OR),
            Map.entry(QuadOp.XOR, OperationType.XOR),
            Map.entry(QuadOp.SUB_I, OperationType.SUB)
    );
    private static final Map<QuadOp, OperationType> GENERAL_QUAD_OP_TO_COMPARISON_OP_MAP = Map.ofEntries(
            Map.entry(QuadOp.LESS_I, OperationType.SETL),
            Map.entry(QuadOp.LESS_EQUAL_I, OperationType.SETLE),
            Map.entry(QuadOp.GREATER_I, OperationType.SETG),
            Map.entry(QuadOp.GREATER_EQUAL_I, OperationType.SETGE),
            Map.entry(QuadOp.EQUAL_I, OperationType.SETE),
            Map.entry(QuadOp.EQUAL_F, OperationType.SETE),
            Map.entry(QuadOp.NOT_EQUAL_F, OperationType.SETNE),
            Map.entry(QuadOp.NOT_EQUAL_I, OperationType.SETNE),
            Map.entry(QuadOp.LESS_F, OperationType.SETB),
            Map.entry(QuadOp.LESS_EQUAL_F, OperationType.SETBE),
            Map.entry(QuadOp.GREATER_F, OperationType.SETA),
            Map.entry(QuadOp.GREATER_EQUAL_F, OperationType.SETAE)
    );

    public InstructionList emitInstructions(SymbolTable symbolTable, Map<String, Constant> constants,TemporaryVariableStack tempStack) throws CompileException{
        InstructionList instructions = new InstructionList();
        switch(op){
            case ADD_I, SUB_I, AND, OR, XOR:{
                instructions.createBinary(tempStack, GENERAL_QUAD_OP_TO_BINARY_OP_MAP.get(this.op), result);
                break;
            }
            case LESS_I, LESS_EQUAL_I, GREATER_I, GREATER_EQUAL_I, EQUAL_I, EQUAL_F,
                 NOT_EQUAL_I, NOT_EQUAL_F, LESS_F, LESS_EQUAL_F, GREATER_F, GREATER_EQUAL_F:{
                instructions.createComparison(tempStack, GENERAL_QUAD_OP_TO_COMPARISON_OP_MAP.get(this.op), operand1.type, result);
                break;
            }
            case ADD_F:{
                instructions.createBinary(tempStack, result.type.isFloat() ? OperationType.ADDSS : OperationType.ADDSD, result);
                break;
            }
            case SUB_F:{
                instructions.createBinary(tempStack, result.type.isFloat() ? OperationType.SUBSS : OperationType.SUBSD, result);
                break;
            }
            case MUL_F:{
                instructions.createBinary(tempStack, result.type.isFloat() ? OperationType.MULSS : OperationType.MULSD, result);
                break;
            }
            case DIV_F:{
                instructions.createBinary(tempStack, result.type.isFloat() ? OperationType.DIVSS : OperationType.DIVSD, result);
                break;
            }
            case ALLOCATE:{
                instructions.allocateStackSpace(Integer.parseInt(operand1.name));
                break;
            }
            case LOGICAL_AND, LOGICAL_OR:{
                instructions.createLogical(symbolTable, tempStack, this.op(), this.result);
                break;
            }
            case PARAM:{
                ArgumentSymbol argumentSymbol = (ArgumentSymbol) operand2;
                if(operand1.type.isStruct()){
                    instructions.moveStruct(symbolTable, tempStack, argumentSymbol, operand1.type);
                }else {
                    if(!argumentSymbol.getExternal()){
                        instructions.popTemporaryIntoPrimary(tempStack);
                        instructions.add(new Instruction(InstructionList.getMoveOpFromType(operand1.type), new Address(Register.RSP, true, argumentSymbol.getOffset()), Register.getPrimaryRegisterFromDataType(operand1.type)));
                    }else{
                        instructions.addExternalParameter(tempStack, argumentSymbol, operand1.type);
                    }
                }
                break;
            }
            case CALL:{
                instructions.add(new Instruction(OperationType.CALL, new Immediate(operand1.name)));

                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                int stackSize = Integer.parseInt(immediateSymbol.getValue());
                stackSize += Compiler.getStackAlignment(stackSize);
                if(stackSize != 0){
                    instructions.add(new Instruction(OperationType.ADD, Register.RSP, new Immediate(stackSize)));
                }
                if(!result.type.isVoid()){
                   instructions.addTemporary(tempStack, result);
                }
                break;
            }
            case JMP :{
                instructions.add(new Instruction(OperationType.JMP, new Immediate(operand1.name)));
                break;
            }
            case LOAD, LOAD_POINTER:{
                VariableSymbol variable = (VariableSymbol) operand1;
                OperationType op = (operand1.type.isStruct() || this.op == QuadOp.LOAD_POINTER) ? OperationType.LEA : InstructionList.getMoveOpFromType(operand1.type);
                instructions.add(new Instruction(op, Register.getPrimaryRegisterFromDataType(result.type), new Address(Register.RBP, true, variable.offset)));
                instructions.addTemporary(tempStack, result);
                break;
            }
            case CONVERT:{
                instructions.convertAddress(tempStack, result, operand1);
                break;
            }
            case LOAD_MEMBER, LOAD_MEMBER_POINTER:{
                ImmediateSymbol memberSymbol    = (ImmediateSymbol) operand2;
                instructions.popTemporaryIntoPrimary(tempStack);
                OperationType operationType = (result.type.isStruct() || this.op == QuadOp.LOAD_MEMBER_POINTER) ? OperationType.LEA : InstructionList.getMoveOpFromType(result.type);
                instructions.add(new Instruction(operationType, Register.getPrimaryRegisterFromDataType(result.type), new Address(Register.RAX, true, Integer.parseInt(memberSymbol.getValue()))));
                instructions.addTemporary(tempStack, result);
                break;
            }
            case STORE_ARRAY_ITEM:{
                VariableSymbol arraySymbol = (VariableSymbol) operand1;
                ArrayDataType arrayDataType = (ArrayDataType)arraySymbol.type;
                String index = ((ImmediateSymbol)operand2).getValue();
                int offset = arraySymbol.offset + Integer.parseInt(index);
                if(arrayDataType.itemType.isStruct()){
                    instructions.popIntoRegister(tempStack, Register.RSI);
                    instructions.add(new Instruction(OperationType.LEA, Register.RDI, new Address(Register.RBP, true, offset)));

                    instructions.createMovSB(SymbolTable.getStructSize(symbolTable.getStructs(), arrayDataType.itemType));
                    break;
                }

                instructions.popTemporaryIntoPrimary(tempStack);
                OperationType moveOp = InstructionList.getMoveOpFromType(arrayDataType.itemType);
                instructions.add(new Instruction(moveOp, new Address(Register.RBP, true, offset), Register.getPrimaryRegisterFromDataType(arrayDataType.itemType)));
                break;
            }
            case REFERENCE_INDEX, INDEX:{
                instructions.calculateIndex(symbolTable, tempStack, operand1.type);
                if(this.op == QuadOp.INDEX){
                    OperationType operationType = result.type.isStruct() ? OperationType.LEA : InstructionList.getMoveOpFromType(result.type);
                    instructions.add(new Instruction(operationType, Register.getPrimaryRegisterFromDataType(result.type), new Address(Register.PRIMARY_GENERAL_REGISTER, true)));
                }
                instructions.addTemporary(tempStack, result);
                break;
            }
            case DEREFERENCE:{
                Register primary = instructions.popTemporaryIntoPrimary(tempStack);
                instructions.add(new Instruction(InstructionList.getMoveOpFromType(result.type), Register.getPrimaryRegisterFromDataType(result.type), new Address(primary, true)));
                instructions.addTemporary(tempStack, result);
                break;
            }
            case JMP_T, JMP_F: {
                instructions.createJumpOnCondition(tempStack, this.op() == QuadOp.JMP_T, operand1);
                break;
            }
            case PRE_INC_F:{
                setupPostfixFloat(tempStack, instructions);
                this.immediateArithmeticFloat(instructions, result.type.isDouble() ? OperationType.ADDSD : OperationType.ADDSS);
                instructions.addTemporary(tempStack, result);
                break;
            }
            case PRE_DEC_F:{
                setupPostfixFloat(tempStack, instructions);
                this.immediateArithmeticFloat(instructions, result.type.isDouble() ? OperationType.SUBSD : OperationType.SUBSS);
                instructions.addTemporary( tempStack, result);
                break;
            }
            case POST_INC_F:{
                setupPostfixFloat(tempStack, instructions);
                instructions.addTemporary(tempStack, result);
                this.immediateArithmeticFloat(instructions, result.type.isDouble() ? OperationType.ADDSD : OperationType.ADDSS);
                break;
            }
            case POST_DEC_F:{
                setupPostfixFloat(tempStack, instructions);
                instructions.addTemporary(tempStack, result);
                this.immediateArithmeticFloat(instructions, result.type.isDouble() ? OperationType.SUBSD : OperationType.SUBSS);
                break;
            }
            case PRE_INC_I, POST_INC_I:{
                this.createPostfixInteger(instructions, tempStack, OperationType.INC, OperationType.ADD, this.op == QuadOp.POST_INC_I);
                break;
            }
            case PRE_DEC_I, POST_DEC_I:{
                this.createPostfixInteger(instructions, tempStack, OperationType.DEC, OperationType.SUB, this.op == QuadOp.POST_DEC_I);
                break;
            }
            case MUL_I:{
                instructions.popTemporaryIntoPrimary(tempStack);
                Register secondary = instructions.popTemporaryIntoSecondary(tempStack);
                instructions.add(new Instruction(OperationType.MUL, new Address(secondary)));
                instructions.addTemporary(tempStack, result);
                break;
            }
            case DIV_I:{
                Register secondary = instructions.popTemporaryIntoSecondary(tempStack);
                instructions.popTemporaryIntoPrimary(tempStack);
                instructions.add(new Instruction(OperationType.XOR, Register.RDX, Register.RDX));
                instructions.add(new Instruction(OperationType.IDIV, new Address(secondary)));
                instructions.addTemporary(tempStack, result);
                break;
            }
            case SHL, SHR:{
                instructions.createShift(tempStack, this.op == QuadOp.SHL ? OperationType.SHL : OperationType.SHR, result);
                break;
            }
            case NEGATE:{
                Register primary = instructions.popTemporaryIntoPrimary(tempStack);
                instructions.add(new Instruction(OperationType.NOT, new Address(primary)));
                instructions.add(new Instruction(OperationType.INC, new Address(primary)));
                instructions.addTemporary(tempStack, result);
                break;
            }
            case NOT_I :{
                Register primary = instructions.popTemporaryIntoPrimary(tempStack);
                instructions.createCompare(OperationType.CMP, OperationType.SETE, primary, new Immediate(0));
                instructions.addTemporary(tempStack, result);
                break;
            }
            case NOT_F :{
                Register primary = instructions.popTemporaryIntoPrimary(tempStack);
                instructions.add(new Instruction(OperationType.MOV, Register.RAX, new Immediate(0)));
                OperationType op = Operation.getCmpOpFromType(operand1.type);
                OperationType moveOp = Operation.getConvertOpFromType(DataType.getInt(), operand1.type);
                instructions.add(new Instruction(moveOp, Register.SECONDARY_SSE_REGISTER, Register.RAX));
                instructions.createCompare(op, OperationType.SETE, primary, new Address(Register.SECONDARY_SSE_REGISTER));
                instructions.addTemporary(tempStack, result);
                break;
            }
            case MOD:{
                instructions.add(new Instruction(OperationType.XOR, Register.RDX, Register.RDX));
                Register secondary = Register.getSecondaryRegisterFromDataType(operand2.type);
                instructions.popIntoRegister(tempStack, secondary);
                instructions.popTemporaryIntoPrimary(tempStack);
                instructions.add(new Instruction(OperationType.IDIV, new Address(secondary)));
                instructions.add(new Instruction(InstructionList.getMoveOpFromType(result.type), Register.getPrimaryRegisterFromDataType(result.type), Register.getThirdRegisterFromDataType(result.type)));
                instructions.addTemporary(tempStack, result);
                break;
            }
            case LABEL:{
                instructions.add(new Instruction(new Label(operand1.name)));
                break;
            }
            case RET_I, RET_F:{
                if(result != null){
                    instructions.popTemporaryIntoPrimary(tempStack);
                }
                instructions.addEpilogue();
                instructions.add(new Instruction(new Operation(OperationType.RET)));
                break;
            }
            case LOAD_IMM_I, LOAD_IMM_F:{
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                if(result.type.isString() || result.type.isFloatingPoint()) {
                    Constant constant = constants.get(immediateSymbol.getValue());
                    instructions.add(new Instruction(InstructionList.getMoveOpFromType(result.type), Register.getPrimaryRegisterFromDataType(result.type), new Immediate(constant.label(), !result.type.isString())));
                }else{
                    instructions.add(new Instruction(InstructionList.getMoveOpFromType(result.type), Register.getPrimaryRegisterFromDataType(result.type), new Immediate(immediateSymbol.getValue())));
                }
                instructions.addTemporary(tempStack, result);
                break;
            }
            case IMUL:{
                Register primary = instructions.popTemporaryIntoPrimary(tempStack);
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                instructions.add(new Instruction(OperationType.IMUL, primary, new Immediate(immediateSymbol.getValue())));
                instructions.addTemporary(tempStack, result);
                break;
            }
            case CAST:{
               break;
            }
            case ASSIGN:{
                if(operand1.type.isStruct()){
                    instructions.assignStruct(symbolTable, tempStack, operand1.type);
                }else{
                    VariableSymbol pointer  = tempStack.popVariable();
                    VariableSymbol value    = tempStack.popVariable();
                    instructions.add(new Instruction(InstructionList.getMoveOpFromType(value.type), Register.getPrimaryRegisterFromDataType(value.type), new Address(Register.RBP, true, value.offset)));
                    instructions.add(new Instruction(InstructionList.getMoveOpFromType(pointer.type), Register.getSecondaryRegisterFromDataType(pointer.type), new Address(Register.RBP, true, pointer.offset)));
                    instructions.add(new Instruction(InstructionList.getMoveOpFromType(operand1.type), new Address(Register.RCX, true), Register.getPrimaryRegisterFromDataType(operand1.type)));
                }

                break;
            }
            default : {throw new CompileException(String.format("Don't know how to make instructions from %s", op.name()));}
        }
        return instructions;
    }

    private void setupPostfixFloat(TemporaryVariableStack tempStack, InstructionList instructions) {
        instructions.popTemporaryIntoPrimary(tempStack);
        instructions.add(new Instruction(InstructionList.getMoveOpFromType(result.type), Register.getPrimaryRegisterFromDataType(operand1.type), new Address(Register.RAX, true)));

        instructions.add(new Instruction(OperationType.MOV, Register.SECONDARY_GENERAL_REGISTER, new Immediate(1)));
        OperationType convertOp = result.type.isDouble() ? OperationType.CVTSI2SD : OperationType.CVTSI2SS;
        instructions.add(new Instruction(convertOp, Register.SECONDARY_SSE_REGISTER, Register.SECONDARY_GENERAL_REGISTER));
    }
    private void immediateArithmeticFloat(InstructionList instructions,  OperationType arithmeticOp){
        OperationType moveOp = InstructionList.getMoveOpFromType(result.type);
        instructions.add(new Instruction(arithmeticOp, Register.PRIMARY_SSE_REGISTER, Register.SECONDARY_SSE_REGISTER));
        instructions.add(new Instruction(moveOp, new Address(Register.RAX, true), Register.PRIMARY_SSE_REGISTER));
    }
    private void createPostfixInteger(InstructionList instructions, TemporaryVariableStack tempStack, OperationType op, OperationType pointerOp, boolean post) throws CompileException {
        instructions.popTemporaryIntoSecondary( tempStack);
        Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
        instructions.add(new Instruction(OperationType.MOV, primary, new Address(Register.RCX, true)));
        if(post){
            instructions.addTemporary(tempStack, result);
        }
        if(operand1.type.isPointer()){
            int pointerSize = operand1.type.getTypeFromPointer().getSize();
            instructions.add(new Instruction(pointerOp, primary, new Immediate(pointerSize)));
        }else{
            instructions.add(new Instruction(op, primary));
        }
        instructions.add(new Instruction(OperationType.MOV, new Address(Register.RCX, true), primary));

        if(!post){
            instructions.addTemporary(tempStack, result);
        }
    }
}
