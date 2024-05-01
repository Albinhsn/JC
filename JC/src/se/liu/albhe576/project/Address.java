package se.liu.albhe576.project;


public class Address extends Operand{
    @Override
    public String toString() {
        if(name != null){
            if(effective){
                return "[" + name + "]";
            }
            return name.toLowerCase();
        }
        else if(effective){
            String out = "[" + register;
            if(offset != 0){
                out += String.format(" %+d", offset);
            }
            out += "]";
            return out.toLowerCase();
        }else{
            assert register != null;
            return register.toString().toLowerCase();
        }
    }
    private final String name;
    private final Register register;
    private final boolean effective;
    private final int offset;
    public Register getRegister(){
        return this.register;
    }

    public Address(String name, boolean effective){
        this.name       = name;
        this.register   = null;
        this.effective  = effective;
        this.offset     = 0;
    }
    public Address(Register register, boolean effective, int offset, String name){
        this.register   = register;
        this.effective  = effective;
        this.offset     = offset;
        this.name       = name;
    }

    public Address(Register register, boolean effective, int offset){
        this(register, effective, offset, null);
    }
    public Address(Register register, boolean effective){
        this(register, effective, 0);
    }
    public Address(Register register){
        this(register, false, 0);
    }
}
