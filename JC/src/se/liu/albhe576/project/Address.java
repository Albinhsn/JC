package se.liu.albhe576.project;

public class Address extends Operand {

    @Override
    public String toString() {
        if(this.effective){
           if(this.offset != 0){
               return String.format("[%s %+d]", register.name().toLowerCase(), this.offset);
           }
           return String.format("[%s]", register.name().toLowerCase());
        }
        return register.name().toLowerCase();
    }

    private final Register register;
    public Address(Register operand, boolean effective, int offset) {
        super(effective, offset);
        this.register = operand;
    }

    public Address(Register  register, boolean effective) {
        super(effective, 0);
        this.register = register;
    }

    public Address(Register register) {
        super(false, 0);
        this.register = register;
    }

    @Override
    OperationType getOp() {
        return null;
    }
}
