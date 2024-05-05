package se.liu.albhe576.project.backend;

/**
 * Class for an operand which is an effective address or an register
 * Is used as an operand for intel instructions
 * Legal instructions have an address in the form of either a label or a register that optionally is effective and have an offset
 * @param <T> The type of address, will mostly be either an immediate string or a register
 * @see Operand
 * @see Instruction
 * @see IntelInstruction
 */
public class Address<T> extends Operand{
    private final T address;
    private final boolean effective;
    private final int offset;

    public Address(T address, boolean effective, int offset){
        this.address    = address;
        this.effective  = effective;
        this.offset     = offset;
    }
    public Address(T address, boolean effective){
        this(address, effective, 0);
    }
    public Address(T address){
        this(address, false, 0);
    }
    @Override
    public String emit() {
        String addressString = address.toString().toLowerCase();
        if(effective){
            if(offset != 0){
                return String.format("[%s %+d]", addressString, offset);
            }
            return String.format("[%s]", addressString);
        }
        return addressString;
    }
    public static Address<?> getEffectiveAddress(Register register){
        return new Address<>(register, true, 0);
    }
    public static Address<?> getEffectiveAddress(Register register, int offset){
        return new Address<>(register, true, offset);
    }

}
