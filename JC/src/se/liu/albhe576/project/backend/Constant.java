package se.liu.albhe576.project.backend;

/**
 * Token record for a constant value for primarily x86 since Strings are preallocated bytes and
 * Floating point immediates are also declared statically (in this language, should be longs though)
 * The label represent the string value of the immediate and the type is the datatype of the constant
 * @param label
 * @param type
 */
public record Constant(String label, DataTypes type) { }
