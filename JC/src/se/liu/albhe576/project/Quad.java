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
            case ADD_I, SUB_I, AND, OR, XOR->{instructions.createBinary(tempStack, GENERAL_QUAD_OP_TO_BINARY_OP_MAP.get(this.op), result);}
            case LESS_I, LESS_EQUAL_I, GREATER_I, GREATER_EQUAL_I, EQUAL_I, EQUAL_F,
                 NOT_EQUAL_I, NOT_EQUAL_F, LESS_F, LESS_EQUAL_F, GREATER_F, GREATER_EQUAL_F->{instructions.createComparison(tempStack, GENERAL_QUAD_OP_TO_COMPARISON_OP_MAP.get(this.op), operand1.type, result);}
            case ADD_F->{instructions.createBinary(tempStack, result.type.isFloat() ? OperationType.ADDSS : OperationType.ADDSD, result);}
            case SUB_F->{instructions.createBinary(tempStack, result.type.isFloat() ? OperationType.SUBSS : OperationType.SUBSD, result);}
            case MUL_F->{instructions.createBinary(tempStack, result.type.isFloat() ? OperationType.MULSS : OperationType.MULSD, result);}
            case DIV_F->{instructions.createBinary(tempStack, result.type.isFloat() ? OperationType.DIVSS : OperationType.DIVSD, result);}
            case ALLOCATE->{instructions.allocateStackSpace(Integer.parseInt(operand1.name));}
            case LOGICAL_AND, LOGICAL_OR->{instructions.createLogical(symbolTable, tempStack, this.op(), this.result);}
            case PARAM->{instructions.createParam(symbolTable, tempStack, (ArgumentSymbol) operand2, operand1);}
            case CALL->{instructions.createCall(tempStack, operand1.name, (ImmediateSymbol) operand2, result);}
            case JMP ->{instructions.createJump(operand1.name);}
            case LOAD, LOAD_POINTER->{instructions.createLoad(tempStack, (VariableSymbol) operand1, this.op, result);}
            case CONVERT->{instructions.convertAddress(tempStack, result, operand1);}
            case LOAD_MEMBER, LOAD_MEMBER_POINTER->{instructions.createLoadMember(tempStack, (ImmediateSymbol) operand2, result, this.op);}
            case STORE_ARRAY_ITEM->{instructions.createStoreArrayItem(symbolTable, tempStack, (VariableSymbol) operand1, (ImmediateSymbol) operand2);}
            case REFERENCE_INDEX, INDEX->{instructions.createIndex(symbolTable, tempStack, result, operand1.type, this.op);}
            case DEREFERENCE->{instructions.createDereference(tempStack, result);}
            case JMP_T, JMP_F->{instructions.createJumpOnCondition(tempStack, this.op() == QuadOp.JMP_T, operand1);}
            case PRE_INC_F->{instructions.createPrefixIncFloat(tempStack, result);}
            case PRE_DEC_F->{instructions.createPrefixDecFloat(tempStack, result);}
            case POST_INC_F->{instructions.createPostfixIncFloat(tempStack, result, operand1.type);}
            case POST_DEC_F->{instructions.createPostfixDecFloat(tempStack, result);}
            case PRE_INC_I, POST_INC_I->{instructions.createPostfixInteger(tempStack, OperationType.INC, OperationType.ADD, this.op == QuadOp.POST_INC_I, result, operand1.type);}
            case PRE_DEC_I, POST_DEC_I->{instructions.createPostfixInteger(tempStack, OperationType.DEC, OperationType.SUB, this.op == QuadOp.POST_DEC_I, result, operand1.type);}
            case MUL_I->{instructions.createIntegerMul(tempStack, result);}
            case DIV_I->{instructions.createIntegerDiv(tempStack, result);}
            case SHL, SHR->{instructions.createShift(tempStack, this.op == QuadOp.SHL ? OperationType.SHL : OperationType.SHR, result);}
            case NEGATE->{instructions.createNegate(tempStack, result);}
            case NOT_I ->{instructions.createNotInteger(tempStack, result);}
            case NOT_F ->{instructions.createNotFloat(tempStack, result, operand1.type);}
            case MOD->{instructions.createMod(tempStack, result, operand1.type);}
            case LABEL->{instructions.addLabel(operand1.name);}
            case RET_I, RET_F->{instructions.createReturn(tempStack, result);}
            case LOAD_IMM_I, LOAD_IMM_F->{instructions.createLoadImmediate(constants, tempStack, (ImmediateSymbol) operand1, result);}
            case IMUL->{instructions.createIMul(tempStack, (ImmediateSymbol) operand2, result);}
            case CAST->{}
            case ASSIGN->{instructions.createAssign(symbolTable, tempStack, operand1.type);}
            case ASSIGN_IMMEDIATE->{instructions.createAssignImmediate((VariableSymbol) operand1, (ImmediateSymbol) operand2);}
            default -> {throw new CompileException(String.format("Don't know how to make instructions from %s", op.name()));}
        }
        return instructions;
    }

}
