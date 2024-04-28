package se.liu.albhe576.project;

public abstract class Operand {
    public boolean isPrimary(){return false;}
    public boolean isPrimaryFloat(){return false;}
    public boolean isPrimaryEffective(){return false;}
    public boolean isPrimaryPointer(){return false;}
    public boolean isSecondaryEffective(){return false;}
    public boolean isImmediate(){return false;}
    public boolean isStackPointer(){return false;}
    public boolean isRSP(){return false;}
    public boolean isSecondary(){return false;}
}
