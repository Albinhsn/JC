package se.liu.albhe576.project;

public enum InstructionType {
    JMP, RET, CALL, ADD, SUB, DIV;

    public static InstructionType getOpType(Token op){
        switch(op.type){

        }
        return RET;
    }
}
