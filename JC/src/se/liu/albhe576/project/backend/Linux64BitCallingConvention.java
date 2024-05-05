package se.liu.albhe576.project.backend;

import static se.liu.albhe576.project.backend.Register.*;

/**
 * The calling convention for linux 64 bit
 * Will pass up to 6 integer or pointer arguments and up to 8 floating point via registers
 * The rest are passed via the stack
 * @see CallingConvention
 */
public class Linux64BitCallingConvention implements CallingConvention
{
    private static final Register[] FLOATING_POINT_REGISTERS =  new Register[]{XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7};
    private static final Register[] GENERAL_REGISTERS = new Register[]{RDI, RSI, RDX, RCX, R8, R9};

    @Override public Register[] getFloatingPointRegisters() {
        return FLOATING_POINT_REGISTERS;
    }

    @Override public Register[] getGeneralRegisters() {
        return GENERAL_REGISTERS;
    }
}
