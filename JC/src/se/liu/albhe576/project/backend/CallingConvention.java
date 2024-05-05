package se.liu.albhe576.project.backend;


/**
 * Simple interface for determening a calling cenvention
 * Defines two functions that should return the register which the function arguments of that type should be placed
 */
public interface CallingConvention
{
    Register[] getFloatingPointRegisters();
    Register[] getGeneralRegisters();

}
