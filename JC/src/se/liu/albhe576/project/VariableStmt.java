package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class VariableStmt implements Stmt{

    @Override
    public String toString() {
        return String.format("%s %s = %s;", type.literal, name, value);
    }

    private final Token type;
    private final String name;
    private final Expr value;
    public VariableStmt(Token type, String name, Expr value){
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, UnexpectedTokenException, InvalidOperation {
        List<Quad> val = value.compile(symbolTable);

        // ToDo check valid conversion instead of just the same

        Symbol lastSymbol = Quad.getLastResult(val);
        DataType varType = DataType.getDataTypeFromToken(type);

        if(!lastSymbol.type.isSameType(varType)){
            throw new CompileException(String.format("Trying to access type %s to type %s", lastSymbol.type.name, varType.name));
        }


        Symbol variable = new Symbol(name, varType);

        val.add(
                new Quad(
                        QuadOp.STORE,
                        lastSymbol,
                        null,
                        variable
                ));
        symbolTable.addSymbol(variable);
        return val;
    }
}
