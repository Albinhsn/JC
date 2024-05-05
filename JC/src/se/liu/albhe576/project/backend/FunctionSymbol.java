package se.liu.albhe576.project.backend;

import se.liu.albhe576.project.frontend.*;
import java.util.List;

/**
 * The symbol for a function. Should only be used in call expressions in order to help define the code generation neccessary for parameters
 * See CallExpression for further explanation of reasoning for this
 * @see CallExpression
 * @see Symbol
 */
public class FunctionSymbol extends Symbol{
    private final int generalCount;
    private final int floatingPointCount;
    private final int stackSpace;
    private final List<StructureField> arguments;

    public int getStackSpace(){
	return this.stackSpace;
    }
    public List<StructureField> getArguments(){
	return this.arguments;
    }
    public int getGeneralCount(){
	return this.generalCount;
    }
    public int getFloatingPointCount(){
	return this.floatingPointCount;
    }
    public FunctionSymbol(final String name, final DataType type, int generalCount, int floatingPointCount, int stackSpace, List<StructureField> arguments) {
	super(name, type);
	this.generalCount 	= generalCount;
	this.floatingPointCount = floatingPointCount;
	this.stackSpace 	= stackSpace;
	this.arguments 		= arguments;
    }
}
