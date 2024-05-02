package se.liu.albhe576.project;

public class Operation {
    @Override
    public String toString() {
        if(size != null){
            return op.name().toLowerCase() + " " + size.name();
        }
        return op.name().toLowerCase();
    }
    private final OperationType op;
    private final OperationSize size;
    OperationType getOp() {return op;}
    public Operation(OperationType op, OperationSize size) {
        this.op     = op;
        this.size   = size;
    }
    public Operation(OperationType op) {this(op, null);}
    public boolean isMove(){return this.op.isMove();}
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
