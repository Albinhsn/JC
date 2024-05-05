package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.*;
/**
 * Is as the name suggests a field within a structure, given a name and a type
 * Is also used to define what is a function argument (though that probably should be it's own type)
 * @param name the name of the field
 * @param type the data type of the field
 * @see Structure
 * @see DataType
 */
public record StructureField(String name, DataType type) { }
