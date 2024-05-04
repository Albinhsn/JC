package se.liu.albhe576.project;

public class Operation extends Operand{
    private final OperationType type;
    public OperationType getType(){
        return type;
    }
    @Override
    protected boolean isRet() {
        return this.type == OperationType.RET;
    }

    @Override
    String emit() {
        return type.name().toLowerCase();
    }
    public Operation(OperationType type){
        this.type = type;
    }

    public static OperationType getMoveOpFromType(DataType type){
        if(!type.isFloatingPoint()){
            return OperationType.MOV;
        }else if(type.isFloat()) {
            return OperationType.MOVSS;
        }
        return OperationType.MOVSD;
    }
}
