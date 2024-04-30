package se.liu.albhe576.project;

public record Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result) {
    @Override
    public String toString() {
        return String.format("%s %s %s %s", op.name(), operand1, operand2, result);
    }

}
