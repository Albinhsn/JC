package se.liu.albhe576.project;

public abstract class Operand {
    @Override
    public String toString() {
        return emit();
    }

    protected boolean isRet(){
        return false;
    }
    abstract String emit();
}


