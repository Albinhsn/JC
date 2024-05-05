package se.liu.albhe576.project.backend;

/**
 * Symbol referring to the item of an array that's been indexed.
 * Extends VariableSymbol since it contains an offset on the stack where the array is decared
 * And then itemOffset that refers to the specific item within the array
 * @see VariableSymbol
 * @see Symbol
 * @see ArrayDataType
 */
public class ArrayItemSymbol extends VariableSymbol{
    public ArrayItemSymbol(String name, DataType type, int offset) {
        super(name, type, offset);
    }
}
