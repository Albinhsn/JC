package se.liu.albhe576.project;

public class Instruction {
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if(label != null){
            return label + ":";
        }
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

    public final Operation op;
    private final Address<?> dest;
    private final Address<?> source;
    private final String label;
    public Instruction(Operation op, Address<?> operand1, Address<?> source, String label) {
        this.op = op;
        this.dest = operand1;
        this.source = source;
        this.label = label;
    }
    public Instruction(Operation op, Address<?> operand1, Address<?> source) {
        this(op, operand1, source, null);
    }
    public Instruction(OperationType op, Address<?> operand1, Address<?> source) {
        this(new Operation(op), operand1, source, null);
    }
    public Instruction(OperationType op, Register operand1, Register source) {
        this(new Operation(op), new Address<>(operand1), new Address<>(source), null);
    }
    public Instruction(OperationType op, Address<?> operand1, Register source) {
        this(new Operation(op), operand1, new Address<>(source), null);
    }
    public Instruction(OperationType op, Register operand1, Address<?> source) {
        this(new Operation(op), new Address<>(operand1), source, null);
    }
    public Instruction(OperationType op, Address<?> operand1) {
        this(new Operation(op), operand1, null, null);
    }
    public Instruction(OperationType op) {
        this(new Operation(op), null, null, null);
    }
    public Instruction(String label) {
        this(null, null, null, label);
    }
}

