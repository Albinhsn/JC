package se.liu.albhe576.project;

public class Immediate extends Operand{
    @Override
    public String toString() {
        return immediate;
    }
    String immediate;
    public Immediate(int imm) {
        this.immediate = String.valueOf(imm);
    }
    public Immediate(String imm) {
        this.immediate = imm;
    }
}
