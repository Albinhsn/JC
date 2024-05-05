package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.SymbolTable;
import se.liu.albhe576.project.backend.VariableSymbol;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.Compiler;
/**
 * An expression which just refers to an identifier
 * @see Expression
 */
public class VariableExpression extends Expression
{
    private final Token token;
    public VariableExpression(Token token, int line, String file){
        super(line, file);
        this.token = token;
    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) {
        VariableSymbol symbol = symbolTable.findSymbol(token.literal());
        if(symbol == null){
            Compiler.panic(String.format("Can't find symbol %s", token.literal()), line, file);
        }
        assert symbol != null;
        intermediates.createLoad(symbolTable, symbol);
    }
}
