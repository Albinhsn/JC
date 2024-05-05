package se.liu.albhe576.project.backend;

import java.util.List;
import se.liu.albhe576.project.frontend.*;

/**
 * The class representing a declared function, either external or user defined.
 * Function can return any type within the language (including user defined ones)
 * varArgs can be defined (for function such as printf), but only for external functions.
 * @see StructureField
 * @see DataType
 * @see Parser
 */
public class Function {

    private final int line;
    private final String file;
    private final List<StructureField> arguments;
    private final DataType returnType;
    private final List<Statement> body;
    private final boolean varArgs;
    public boolean isVarArgs(){
        return varArgs;
    }
    public String getFile(){
        return this.file;
    }
    public int getLine(){
        return this.line;
    }
    public List<StructureField> getArguments(){return this.arguments;}
    public boolean isExternal(){return this.body == null;}
    public List<Statement> getBody(){return this.body;}
    public DataType getReturnType(){return this.returnType;}
    public FunctionSymbol getFunctionSymbol(String name, int generalCount, int floatCount, int stackSpace){return new FunctionSymbol(name, returnType, generalCount, floatCount, stackSpace, arguments);}
    public Function(List<StructureField> arguments, DataType returnType, List<Statement> statements, String file, int line, boolean varArgs){
        this.arguments = arguments;
        this.returnType = returnType;
        this.body = statements;
        this.file = file;
        this.line = line;
        this.varArgs = varArgs;
    }

}
