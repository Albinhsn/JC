package se.liu.albhe576.project;

import java.util.Map;

public enum OperationSize {
    QWORD, DWORD, WORD, BYTE;

    private static final Map<DataTypes, OperationSize> DATA_TYPE_TO_SIZE_MAP = Map.of(
           DataTypes.LONG, QWORD,
           DataTypes.DOUBLE, QWORD,
           DataTypes.INT, DWORD,
           DataTypes.FLOAT, DWORD,
           DataTypes.SHORT, WORD,
            DataTypes.BYTE, BYTE
    );

    public static OperationSize getSizeFromType(DataType type){
        return DATA_TYPE_TO_SIZE_MAP.get(type.type);
    }
}
