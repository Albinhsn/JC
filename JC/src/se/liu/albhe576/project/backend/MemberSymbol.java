package se.liu.albhe576.project.backend;

/**
 * The symbol for a member variable which is a field within a struct
 * The offset defined should be the offset which the field exists from the base pointer (not where the struct starts)
 */
public class MemberSymbol extends Symbol{
    private final int offset;
    public int getOffset(){
        return this.offset;
    }

    public MemberSymbol(String name, DataType type, int offset) {
        super(name, type);
        this.offset = offset;
    }
}
