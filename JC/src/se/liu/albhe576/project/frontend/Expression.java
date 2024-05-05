package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.*;
/**
 * Base class for expressions
 * Contains line and file fields for debugging and error handling purposes
 */
public abstract class Expression{
    protected final int line;
    protected final String file;
    abstract public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException;
    protected Expression(int line, String file){
        this.line = line;
        this.file = file;
    }

}
