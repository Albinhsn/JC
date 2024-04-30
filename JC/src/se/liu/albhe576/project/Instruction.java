package se.liu.albhe576.project;

public class Instruction {
    String label;
    Operation op;
    Operand operand1;
    Operand operand2;
    public Instruction(Operation op, Operand operand1, Operand operand2){
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }
}
