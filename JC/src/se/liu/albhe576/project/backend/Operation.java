package se.liu.albhe576.project.backend;

/**
 * Defines every type of operation (within x86)
 * Has a bunch of static helpers function for conversions via their respective types
 * @see IntelInstruction
 * @see Instruction
 * @see DataType
 */
public enum Operation
{
    MOV, MOVSS, MOVSD, LEA, REP,NOT, CVTSI2SS, CVTSI2SD, CVTSD2SS, CVTTSD2SI, CVTSS2SI, CVTSS2SD, MOVSB,
    ADD, ADDSS, ADDSD, SUB, SUBSS, SUBSD, MUL, MULSS, MULSD, IDIV, DIVSS, DIVSD, IMUL,
    INC, DEC, SHL, SHR, AND, OR, XOR, PUSH, POP, CMP, COMISS, COMISD, JMP, JE, SETLE, SETG, SETGE, SETL, SETE, SETA, SETNE, SETAE, SETB, SETBE, MOVSX, CALL, RET;

    public Operation convertToDouble() throws CompileException {
        return convertOperation(ADDSD, SUBSD, MULSD, DIVSD, COMISD);
    }
    public static Operation getOpFromResultType(IntermediateOperation intermediateOperation, DataType target) throws CompileException {
        Operation op = getBinaryOpFromIntermediateOp(intermediateOperation);
        return target.isFloatingPoint() ? op.convertToFloatingPoint(target) : op;
    }
    public static Operation getMoveOpFromType(DataType type){
        if(!type.isFloatingPoint()){
            return Operation.MOV;
        }else if(type.isFloat()) {
            return Operation.MOVSS;
        }
        return Operation.MOVSD;
    }
    public static Operation getCmpOpFromType(DataType type){
        if(!type.isFloatingPoint()){
            return Operation.CMP;
        }else if(type.isFloat()) {
            return Operation.COMISS;
        }
        return Operation.COMISD;
    }

    public static Operation getConvertOpFromType(DataType source, DataType type){
        if(type.isFloat() && source.isInteger()){
            return Operation.CVTSI2SS;
        }
        else if(type.isDouble() && source.isInteger()){
            return Operation.CVTSI2SD;
        }
        else if((type.isInteger() || type.isPointer()) && source.isFloat()){
            return Operation.CVTSS2SI;
        }
        else if(type.isInteger() && source.isDouble()){
            return Operation.CVTTSD2SI;
        }
        else if(type.isFloat() && source.isDouble()){
            return Operation.CVTSD2SS;
        }
        else if(type.isDouble() && source.isFloat()){
            return Operation.CVTSS2SD;
        }
        return Operation.MOVSX;
    }
    public Operation convertToFloatingPoint(DataType target) throws CompileException {
        if(target.isDouble()){
            return this.convertToDouble();
        }
        return this.convertToFloat();
    }
    public Operation convertToFloat() throws CompileException {
        return convertOperation(ADDSS, SUBSS, MULSS, DIVSS, COMISS);
    }

    private Operation convertOperation(final Operation addOp, final Operation subOp, final Operation mulOp,
                                       final Operation divOp, final Operation cmpOp) throws CompileException
    {
        switch(this){
            case ADD, INC ->{return addOp;}
            case SUB,DEC ->{return subOp;}
            case MUL ->{return mulOp;}
            case IDIV ->{return divOp;}
            case CMP ->{return cmpOp;}
            case SETL ->{return SETB;}
            case SETLE ->{return SETBE;}
            case SETG ->{return SETA;}
            case SETGE ->{return SETAE;}
            case SETE ->{return SETE;}
            case SETNE ->{return SETNE;}
        }
        throw new CompileException(String.format("Invalid intermediate op to binary op %s", this.name()));
    }

    public static Operation getBinaryOpFromIntermediateOp(IntermediateOperation op) throws CompileException {
        switch(op){
            case PRE_INC, POST_INC ->{return INC;}
            case PRE_DEC,POST_DEC ->{return DEC;}
            case ADD->{return ADD;}
            case SUB ->{return SUB;}
            case MUL ->{return MUL;}
            case DIV ->{return IDIV;}
            case SHL ->{return SHL;}
            case SHR ->{return SHR;}
            case AND ->{return AND;}
            case OR ->{return OR;}
            case XOR ->{return XOR;}
            case LESS ->{return SETL;}
            case LESS_EQUAL ->{return SETLE;}
            case GREATER ->{return SETG;}
            case GREATER_EQUAL->{return SETGE;}
            case EQUAL->{return SETE;}
            case NOT_EQUAL->{return SETNE;}
        }
        throw new CompileException(String.format("Invalid intermediate op to binary op %s", op.name()));
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
