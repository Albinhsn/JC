package se.liu.albhe576.project.frontend;

import java.util.List;
import se.liu.albhe576.project.backend.*;

/**
 * Base class for statements
 * Contains line and file fields for debugging and error handling purposes
 */
public abstract class Statement {
    protected final int line;
    protected final String file;
    protected Statement(int line, String file){
        this.line = line;
        this.file = file;
    }
    abstract public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException;
    public static void compileBlock(SymbolTable symbolTable, IntermediateList intermediates, List<Statement> block) throws CompileException {
        symbolTable.enterScope();
        for(Statement statement : block){
            statement.compile(symbolTable, intermediates);
        }
        symbolTable.exitScope();
    }
}
