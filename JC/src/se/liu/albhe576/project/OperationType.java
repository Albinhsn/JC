package se.liu.albhe576.project;

public enum OperationType {
    MOV, MOVSS, MOVSD, LEA, REP,NOT, CVTSI2SS, CVTSI2SD, CVTSD2SS, CVTTSD2SI, CVTSS2SI, CVTSS2SD, MOVSB,
    ADD, ADDSS, ADDSD, SUB, SUBSS, SUBSD, MUL, MULSS, MULSD, IDIV, DIVSS, DIVSD, IMUL,
    INC, DEC, SHL, SHR, AND, OR, XOR, PUSH, POP, CMP, COMISS, COMISD, JMP, JE, SETLE, SETG, SETGE, SETL, SETE, SETA, SETNE, SETAE, SETB, SETBE, MOVSX, CALL, RET;

    public OperationType convertToDouble() throws CompileException {
        switch(this){
            case ADD, INC->{return ADDSD;}
            case SUB, DEC ->{return SUBSD;}
            case MUL ->{return MULSD;}
            case IDIV ->{return DIVSD;}
            case CMP ->{return COMISD;}
            case SETL ->{return SETB;}
            case SETLE ->{return SETBE;}
            case SETG ->{return SETA;}
            case SETGE ->{return SETAE;}
            case SETE ->{return SETE;}
            case SETNE ->{return SETNE;}
        }
        throw new CompileException(String.format("Invalid quad op to binary op %s", this.name()));
    }
    public static OperationType getOpFromResultType(QuadOp quadOp, DataType target) throws CompileException {
        OperationType op = getBinaryOpFromQuadOp(quadOp);
        if(target.isFloatingPoint()){
            op.convertToFloatingPoint(target);
        }
        return op;

    }
    public static OperationType getMoveOpFromType(DataType type){
        if(!type.isFloatingPoint()){
            return OperationType.MOV;
        }else if(type.isFloat()) {
            return OperationType.MOVSS;
        }
        return OperationType.MOVSD;
    }
    public static OperationType getCmpOpFromType(DataType type){
        if(!type.isFloatingPoint()){
            return OperationType.CMP;
        }else if(type.isFloat()) {
            return OperationType.COMISS;
        }
        return OperationType.COMISD;
    }

    public static OperationType getConvertOpFromType(DataType source, DataType type){
        if(type.isFloat() && source.isInteger()){
            return OperationType.CVTSI2SS;
        }
        if(type.isDouble() && source.isInteger()){
            return OperationType.CVTSI2SD;
        }
        if((type.isInteger() || type.isPointer()) && source.isFloat()){
            return OperationType.CVTSS2SI;
        }
        if(type.isInteger() && source.isDouble()){
            return OperationType.CVTTSD2SI;
        }
        if(type.isFloat() && source.isDouble()){
            return OperationType.CVTSD2SS;
        }
        if(type.isDouble() && source.isFloat()){
            return OperationType.CVTSS2SD;
        }
        return OperationType.MOVSX;
    }
    public OperationType convertToFloatingPoint(DataType target) throws CompileException {
        if(target.isDouble()){
            return this.convertToDouble();
        }
        return this.convertToFloat();
    }
    public OperationType convertToFloat() throws CompileException {
        switch(this){
            case ADD, INC ->{return ADDSS;}
            case SUB,DEC ->{return SUBSS;}
            case MUL ->{return MULSS;}
            case IDIV ->{return DIVSS;}
            case CMP ->{return COMISS;}
            case SETL ->{return SETB;}
            case SETLE ->{return SETBE;}
            case SETG ->{return SETA;}
            case SETGE ->{return SETAE;}
            case SETE ->{return SETE;}
            case SETNE ->{return SETNE;}
        }
        throw new CompileException(String.format("Invalid quad op to binary op %s", this.name()));
    }
    public static OperationType getBinaryOpFromQuadOp(QuadOp op) throws CompileException {
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
        throw new CompileException(String.format("Invalid quad op to binary op %s", op.name()));
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
