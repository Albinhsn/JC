package se.liu.albhe576.project;

public class ImmediateSymbol extends Symbol{

    @Override
    public String toString() {
        return value.literal;
    }

    public Token value;
    public ImmediateSymbol(Token value){
       super("imm");
       this.value = value;
    }

}
