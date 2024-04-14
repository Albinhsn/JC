package se.liu.albhe576.project;

import java.util.List;

public class Quad {

    @Override
    public String toString() {
        return op.name() + "\t\t" +
                operand1 + "\t\t" +
                operand2 + "\t\t" +
                result;
    }

    public QuadOp op;
    public Symbol operand1;
    public Symbol operand2;
    public Symbol result;

    public static Symbol getLastResult(List<Quad> quads){
        return quads.get(quads.size() - 1).result;
    }

    public Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.result   = result;

    }
}
