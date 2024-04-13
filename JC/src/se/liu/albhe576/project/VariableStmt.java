package se.liu.albhe576.project;

import java.util.List;

public class VariableStmt implements Stmt{

    @Override
    public String toString() {
        return String.format("%s %s = %s;", type, name, value);
    }

    private final SymbolType type;
    private final String name;
    private final Expr value;
    public VariableStmt(SymbolType type, String name, Expr value){
        this.type = type;
        this.name = name;
        this.value = value;
    }

    @Override
    public Signature getSignature() throws CompileException {
        throw new CompileException("Can't get signature from this stmt");
    }

    @Override
    public BasicBlock compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException {
        // ToDo figure out struct fields of this and if it's correct?
        Value val = value.compile(functions, block, symbols);
        List<Symbol> lastSymbols = symbols.get(symbols.size() - 1);
        lastSymbols.add(new Symbol(type, null, name, val));
        return block;
    }
}
