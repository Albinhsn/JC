package se.liu.albhe576.project;

public enum OperationType {
    MOV, MOVSS, MOVSD, LEA, REP,NOT, CVTSI2SS, CVTSI2SD, CVTSD2SS, CVTTSD2SI, CVTSS2SI, CVTSS2SD, MOVSB,
    ADD, ADDSS, ADDSD, SUB, SUBSS, SUBSD, MUL, MULSS, MULSD, IDIV, DIVSS, DIVSD, IMUL,
    INC, DEC, SHL, SHR, AND, OR, XOR, PUSH, POP, CMP, COMISS, COMISD, JMP, JE, SETLE, SETG, SETGE, SETL, SETE, SETA, SETNE, SETAE, SETB, SETBE, MOVSX, CALL, RET;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
    public boolean isMove(){return this == MOV || this == MOVSS || this == MOVSD;}
}
