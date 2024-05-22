package se.liu.albhe576.project.backend;

import static se.liu.albhe576.project.backend.Operation.*;
import static se.liu.albhe576.project.backend.Register.*;

/**
 * The instructionlist implementation for x86 assembly
 * @see IntelInstruction
 * @see InstructionList
 * @see Register
 * @see Operation
 */
public class IntelInstructionList extends InstructionList{
    public void createCall(String name){
	this.instructions.add(new IntelInstruction(CALL, new Immediate<>(name)));
    }
    public void createReturn(){
        this.instructions.add(new IntelInstruction(RET));
    }
    public void createIntegerDivision(){
	this.addInstruction(XOR, RDX, RDX);
	this.addInstruction(IDIV, SECONDARY_GENERAL_REGISTER);
    }
    public void addInstruction(int index, IntelInstruction instruction){
	this.instructions.add(index, instruction);
    }
    public void createJump(String label){
	this.instructions.add(new IntelInstruction(JMP, new Immediate<>(label)));
    }
    public void addInstruction(Operation op, Register dest){
	this.instructions.add(new IntelInstruction(op, new Address<>(dest), null));
    }
    public void addInstruction(Operation op, Operand dest){
	this.instructions.add(new IntelInstruction(op, dest, null));
    }
    public void addInstruction(Operation op, Operand dest, Register source){
	this.instructions.add(new IntelInstruction(op, dest, new Address<>(source)));
    }
    public void addInstruction(Operation op, Register destRegister, Operand source){
	Address<?, ?> dest     = destRegister == null ? null : new Address<>(destRegister);
	this.instructions.add(new IntelInstruction(op, dest, source));
    }
    public void addInstruction(Operation op, Register dest, Register source){
	this.instructions.add(new IntelInstruction(op, new Address<>(dest), new Address<>(source)));
    }
    public void addInstruction(Operation op, Operand dest, Operand source){
	this.instructions.add(new IntelInstruction(op, dest, source));
    }

    public void addLabel(String label){
	this.instructions.add(new Label(label));
    }
    public void addPrologue(){
	this.addInstruction(PUSH, RBP);
	this.addInstruction(MOV, RBP, RSP);
    }
    public void addEpilogue(){
	this.addInstruction(MOV, RSP, RBP);
	this.addInstruction(POP, RBP);
    }

    public void postfixArithmeticFloat(Operation arithmeticOp, DataType result){
	// do the arithmetic op with sse registers
	Operation moveOp = Operation.getMoveOpFromType(result);
	addInstruction(arithmeticOp, PRIMARY_SSE_REGISTER, SECONDARY_SSE_REGISTER);
	// then store the value into the pointer which is in general primary
	addInstruction(moveOp, Address.getEffectiveAddress(PRIMARY_GENERAL_REGISTER), PRIMARY_SSE_REGISTER);
    }

    public void createMove(DataType destinationType, Address<?, ?> destination, Operand source){
	addInstruction(getMoveOpFromType(destinationType), destination, source);
    }
    public void createMoveIntoPrimary(DataType destinationType, Operand source) throws CompileException {
	createMove(destinationType, new Address<>(getPrimaryRegisterFromDataType(destinationType)), source);
    }
    public void createCompare(Operation compareOp, Operation setOp, Register left, Operand right){
	addInstruction(compareOp, left, right);
	addInstruction(setOp, new Address<>(AL));
    }
    public void deallocateStackSpace(int space){
	addInstruction(ADD, RSP, new Immediate<>(space));
    }
    public void createLoadEffectiveAddress(Register destination, Address<?, ?> source){
	addInstruction(LEA, destination, source);
    }
    public void createSignExtend(Register dest, Operand source){
	addInstruction(MOVSX, dest, source);
    }
    public void createMoveStringByte(int bytesToMove){
	addInstruction(MOV, RCX, new Immediate<>(bytesToMove));
	addInstruction(REP, new Address<>(MOVSB));
    }
}
