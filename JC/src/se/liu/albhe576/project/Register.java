package se.liu.albhe576.project;

public class Register extends Address{

    @Override
    public boolean equals(Object obj) {
        Address address = (Address) obj;
        return address.isEqual(this);
    }

    @Override
    public boolean isPrimary() {
        return (this.type == RegisterType.RAX || this.type == RegisterType.AX || this.type == RegisterType.AL || this.type == RegisterType.EAX) && this.offset == 0 && !this.effective;
    }
    @Override
    public boolean isPrimaryFloat() {
        return (this.type == RegisterType.XMM0) && this.offset == 0 && !this.effective;
    }
    @Override
    public boolean isSecondaryFloat() {
        return (this.type == RegisterType.XMM1) && this.offset == 0 && !this.effective;
    }
    @Override
    public boolean isPrimaryEffective() {
        return (this.type == RegisterType.RAX || this.type == RegisterType.AX || this.type == RegisterType.AL || this.type == RegisterType.EAX) && this.offset == 0 && this.effective;
    }
    @Override
    public boolean isPrimaryPointer() {
        return (this.type == RegisterType.RAX) && this.effective;
    }
    @Override
    public boolean isSecondaryEffective() {
        return (this.type == RegisterType.RCX || this.type == RegisterType.CX || this.type == RegisterType.CL || this.type == RegisterType.ECX) && this.offset == 0 && this.effective;
    }
    @Override
    public boolean isStackPointer() {
        return this.effective && (this.type == RegisterType.RBP || this.isRSP());
    }

    @Override
    public boolean isSecondary() {
        return (this.type == RegisterType.RCX || this.type == RegisterType.CX || this.type == RegisterType.CL || this.type == RegisterType.ECX) && this.offset == 0 && !this.effective;
    }
    @Override
    public boolean shouldBeExtendedPrimary() {
        return this.type == RegisterType.AX || this.type == RegisterType.AL;
    }

    @Override
    public boolean isRSP() {
        return this.type == RegisterType.RSP;
    }
    @Override
    public boolean isRSPEffective() {
        return this.isRSP() && this.effective;
    }
    @Override
    public boolean isEffective() {
        return this.effective;
    }

    @Override
    boolean isEqual(Register register) {return register.type == this.type && register.offset == this.offset;}
    @Override
    boolean isEqual(Label label) {return false;}

    @Override
    public String toString() {
        if(effective){
            if(offset != 0){
                return String.format("[%s %+d]", type.name(), offset).toLowerCase();
            }
            return String.format("[%s]", type.name()).toLowerCase();
        }
        return type.name().toLowerCase();
    }

    public RegisterType type;

    public Register(RegisterType type){
        this.type = type;
    }
    public Register(RegisterType type, int offset){
        this(type, offset, true);
    }
    public Register(RegisterType type, boolean effective){
        this(type, 0, effective);
    }
    public Register(RegisterType type, int offset, boolean effective){
        this.type = type;
        this.offset = offset;
        this.effective = effective;
    }
}
