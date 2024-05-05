package se.liu.albhe576.project.backend;

/**
 * A temporary variable is a variable that is the result of an expression but has not yet been consumed
 * The offset is its location from the base pointer and should be above every defined variable
 * @param offset the offset from the stack it's location
 * @param type the type it has
 * @see DataType
 */
public record TemporaryVariable(int offset, DataType type){}
