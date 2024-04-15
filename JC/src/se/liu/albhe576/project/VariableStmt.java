package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class VariableStmt implements Stmt{

    @Override
    public String toString() {
        return String.format("%s %s = %s;", structName, name, value);
    }

    private final String structName;
    private final String name;
    private final Expr value;
    public VariableStmt(String structName, String name, Expr value){
        this.name = name;
        this.structName = structName;
        this.value = value;
    }

    @Override
    public List<Quad> compile(List<StructSymbol> structTable, Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        List<Quad> val = value.compile(symbolTable);
        // Check for 0?
        Symbol lastSymbol = Quad.getLastResult(val);
        Symbol variable = new VariableSymbol(Compiler.lookupStruct(structTable, structName), name);
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
