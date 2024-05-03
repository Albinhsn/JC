package se.liu.albhe576.project;

public class Operation extends Operand{
    private final OperationType type;
    private final OperationSize size;

    public OperationType getType(){
        return type;
    }

    @Override
    protected boolean isRet() {
        return this.type == OperationType.RET;
    }

    @Override
    String emit() {
        String typeString = type.name().toLowerCase();
        if(size != null){
            return  typeString + " " + size;
        }
        return typeString;
    }
    public Operation(OperationType type, OperationSize size){
       this.type = type;
       this.size = size;
    }
    public Operation(OperationType type){
        this.type = type;
        this.size = null;
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
