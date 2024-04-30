package se.liu.albhe576.project;

public class Register extends Address{

    public RegisterType type;
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
