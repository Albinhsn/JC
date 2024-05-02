package se.liu.albhe576.project;

public class Instruction {
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (op != null) {
            stringBuilder.append("\t").append(op);
        }
        if (dest != null) {
            stringBuilder.append(" ").append(dest);
        }
        if (source != null) {
            stringBuilder.append(", ").append(source);
        }
        return stringBuilder.toString();
    }

    private final Operand op;
    private final Operand dest;
    private final Operand source;

    public Operand getOp() {
        return op;
    }

    public Instruction(OperationType op, Operand operand1, Operand source) {
        this.op = new Operation(op);
        this.dest = operand1;
        this.source = source;
    }
    public Instruction(OperationType op, Register operand1, Operand source) {
        this.op = new Operation(op);
        this.dest = new Address(operand1);
        this.source = source;
    }
    public Instruction(OperationType op, Operand dest, Register source) {
        this.op = new Operation(op);
        this.source = new Address(source);
        this.dest = dest;
    }
    public Instruction(OperationType op, Register dest, Register source) {
        this.op = new Operation(op);
        this.source = new Address(source);
        this.dest = new Address(dest);
    }

    public Instruction(OperationType op, Operand operand1) {
        this.op = new Operation(op);
        this.dest = operand1;
        this.source = null;
    }

    public Instruction(Operand op) {
        this.op = op;
        this.dest = null;
        this.source = null;
    }
}

