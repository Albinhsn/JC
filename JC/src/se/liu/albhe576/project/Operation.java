package se.liu.albhe576.project;

public enum Operation {
    MOV, MOVSS, MOVSD, LEA, REP,NOT,
    CVTSI2SS, CVTSI2SD, CVTSD2SS, CVTTSD2SI, CVTSS2SI, CVTSS2SD,
    ADD, ADDSS, ADDSD, SUB, SUBSS, SUBSD, MUL, MULSS, MULSD, IDIV, DIVSS, DIVSD, IMUL,
    INC, DEC, SHL, SHR, AND, OR, XOR, PUSH, POP,
    CMP, COMISS, COMISD,
    JMP, JNZ, JE, JL, JLE, JG, JGE, JA, JAE, JB, JBE, JNE,
    SETLE, SETG, SETGE, SETL, SETE, SETA, SETNE, SETAE, SETB, SETBE,
    MOVSX, CALL, RET;

    public boolean isJumping(){
        return this == JMP || this == JNZ || this == JE || this == JL || this == JLE || this == JG || this == JGE || this == JA || this == JAE || this == JB || this == JBE || this == JNE || this == CALL || this == RET;
    }
    public boolean isMove(){
        return this == MOV || this == MOVSS || this == MOVSD;
    }
}
