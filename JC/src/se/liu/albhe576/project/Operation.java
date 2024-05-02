package se.liu.albhe576.project;

public class Operation extends Operand{
    @Override
    public String toString() {
        return op.name().toLowerCase();
    }
    private final OperationType op;
    @Override
    OperationType getOp() {
        return op;
    }
    public Operation(OperationType op, boolean effective, int offset) {
        super(effective, offset);
        this.op = op;
    }
    public Operation(OperationType op) {
        this(op, false, 0);
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

}
