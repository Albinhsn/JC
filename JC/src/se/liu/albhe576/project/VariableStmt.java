package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class VariableStmt implements Stmt{

    @Override
    public String toString() {
        return String.format("%s %s = %s;", type, name, value);
    }

    private final StructType type;
    private final String name;
    private final Expr value;
    public VariableStmt(StructType type, String name, Expr value){
        this.type = type;
        this.name = name;
        this.value = value;
    }

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        List<Quad> val = value.compile(symbolTable);
        // Check for 0?
        Symbol lastSymbol = Quad.getLastResult(val);
        Symbol variable = new VariableSymbol(type, name);
        val.add(
                new Quad(
                        QuadOp.STORE,
                        lastSymbol,
                        null,
                        variable
                ));
        symbolTable.peek().add(variable);
        return val;
    }
}
