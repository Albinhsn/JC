package se.liu.albhe576.project;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Optimizer {

    @FunctionalInterface
    private interface OptimizeFunction{
        boolean optimize(LinkedList<Instruction> instructions, int i) throws CompileException;
    }


    private final List<OptimizeFunction> optimizationFunctions = new ArrayList<>(
            List.of(this::optimizeStore,
                this::optimizeAddImmediate,
                this::optimizeLoadEffective,
                this::optimizeLoad,
                this::optimizeLoadFloat,
                this::optimizePushAndPopFloat,
                this::optimizePush,
                this::optimizeStoreImmediate,
                this::optimizeSetupFloatingPointBinary,
                this::optimizePointerArithmetic,
                this::optimizeIndexing,
                this::optimizeSetupBinary,
                this::optimizeSignExtendImmediateMove,
                this::optimizeStackAligning,
                this::optimizeSignExtend,
                this::optimizePush
            )
    );

    private final Map<String, Point> removedMap;
    public Optimizer(Map<String, Point> removedMap){
        this.removedMap = removedMap;
    }

    public void optimize(LinkedList<Instruction> instructions) throws CompileException {
        int i = 0;
        while (i < instructions.size() - 1) {
            boolean found = false;
            for (OptimizeFunction x : this.optimizationFunctions) {
                if (x.optimize(instructions, i)) {
                    i = 0;
                    found = true;
                    break;
                }
            }
            if(!found){
                i++;
            }
        }
    }

    private boolean optimizeStackAligning(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 2){
            return false;
        }

        Instruction first = instructions.get(i);
        Instruction second = instructions.get(i + 1);
        if(!(first.op == Operation.ADD || first.op == Operation.SUB) || !first.operand0.isRSP()){
            return false;
        }

        if(!(second.op == Operation.ADD || second.op == Operation.SUB) || !second.operand0.isRSP()){
            return false;
        }

        Immediate fstImm = (Immediate) first.operand1;
        Immediate sndImm = (Immediate) second.operand1;

        int fstSize = Integer.parseInt(fstImm.immediate);
        fstSize = first.op == Operation.ADD ? fstSize : -fstSize;
        int sndSize = Integer.parseInt(sndImm.immediate);
        sndSize = second.op == Operation.ADD ? sndSize : -sndSize;
        int newSize = fstSize + sndSize;

        Immediate newImm = new Immediate(String.valueOf(Math.abs(newSize)));
        Operation newOp = newSize < 0 ? Operation.SUB : Operation.ADD;

        instructions.remove(i + 1);
        instructions.remove(i);
        int removed = 2;
        if (newSize != 0) {
            instructions.add(i, new Instruction(newOp, second.operand0, newImm));
            removed--;
        }
        String name = "Stack aligning";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y += removed;
        this.removedMap.put(name, current);
        return true;
    }

    private boolean optimizePush(LinkedList<Instruction> instructions, int start){

        Instruction curr = instructions.get(start);
        Instruction next = instructions.get(start + 1);
        if (curr.op != Operation.SUB || !curr.operand0.isRSP() || next.op != Operation.PUSH || next.operand0.isRSP()) {
            return false;
        }

        Instruction prev;
        int i = start + 2;
        while(i < instructions.size()){
            prev = curr;
            curr = instructions.get(i);
            if(curr.op.isJumping()){
                return false;
            }
            if(prev.op == Operation.SUB && prev.operand0.isRSP() && curr.op == Operation.PUSH){
                return false;
            }
            else if(prev.op == Operation.POP && prev.operand0.isPrimary() && curr.op == Operation.ADD && curr.operand0.isRSP()){
                instructions.remove(i);
                instructions.remove(start);

                String name = "Push";
                Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
                current.x++;
                current.y++;
                this.removedMap.put(name, current);
                return true;
            }
            i++;
        }
        return false;
    }
    private boolean optimizeLoad(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 2){
            return false;
        }

        Instruction first = instructions.get(i);
        if(!(first.op == Operation.MOV || first.op == Operation.LEA) || !first.operand0.isPrimary()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.MOV || !second.operand0.isSecondary() || !second.operand1.isPrimary()){
            return false;
        }

        instructions.remove(i + 1);
        instructions.remove(i);
        instructions.add(i, new Instruction(first.op, second.operand0, first.operand1));
        String name = "Load";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y++;
        this.removedMap.put(name, current);
        return true;
    }

    private boolean optimizeLoadEffective(LinkedList<Instruction> instructions, int i){

        if(instructions.size() <= i + 2){
            return false;
        }

        Instruction first = instructions.get(i);
        if(first.op != Operation.LEA || !first.operand0.isPrimary() || !first.operand1.isEffective()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(!(second.op.isMove() || second.op == Operation.LEA) || !(second.operand1.isPrimaryPointer() || second.operand1.isPrimaryEffective())){
            return false;
        }

        Register reg0 = (Register) first.operand1;
        Register reg1 = (Register) second.operand1;

        int newEffective = reg0.offset + reg1.offset;
        instructions.remove(i + 1);
        instructions.remove(i);

        instructions.add(i, new Instruction(second.op, second.operand0, new Register(reg0.type, newEffective, true)));
        String name = "Load Effective";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y++;
        this.removedMap.put(name, current);
        return true;
    }
    private boolean optimizePushAndPopFloat(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 4){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.SUB || !first.operand0.isRSP() || !first.operand1.isImmediate()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(!second.op.isMove() || !second.operand0.isRSPEffective() || !second.operand1.isPrimaryFloat()){
            return false;
        }
        Instruction third = instructions.get(i + 2);
        if(!third.op.isMove() || !third.operand0.isPrimaryFloat() || !third.operand1.isRSPEffective()){
            return false;
        }
        Instruction fourth = instructions.get(i + 3);
        if(fourth.op != Operation.ADD || !fourth.operand0.isRSP() || !fourth.operand1.isImmediate()){
            return false;
        }

        instructions.remove(i + 3);
        instructions.remove(i + 2);
        instructions.remove(i + 1);
        instructions.remove(i);

        String name = "Push and Pop Float";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y += 3;
        this.removedMap.put(name, current);

        return true;
    }
    private boolean optimizeStoreImmediate(LinkedList<Instruction> instructions, int i) throws CompileException {
        if(instructions.size() <= i + 2){
            return false;
        }

        Instruction first = instructions.get(i);
        if(first.op != Operation.MOV  || !first.operand0.isPrimary() || !first.operand1.isImmediate()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.MOV || !second.operand0.isStackPointer() || !second.operand1.isPrimary()){
            return false;
        }

        Immediate immediate = (Immediate) first.operand1;
        Register source = (Register) second.operand1;
        OperationSize size = OperationSize.getSizeFromRegister(source);
        instructions.remove(i + 1);
        instructions.remove(i);
        instructions.add(i, new Instruction(second.op, size, second.operand0, immediate));

        String name = "Store Immediate";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y++;
        this.removedMap.put(name, current);

        return true;
    }
    private boolean optimizeAddImmediate(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 2){
            return false;
        }

        Instruction first = instructions.get(i);
        if(first.op != Operation.MOV  || !first.operand0.isSecondary() || !first.operand1.isImmediate()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(!(second.op == Operation.ADD || second.op == Operation.SUB)|| !(second.operand0.isSecondary() || second.operand0.isPrimary()) || !(second.operand1.isPrimary() || second.operand1.isSecondary())){
            return false;
        }

        Immediate immediate = (Immediate) first.operand1;
        instructions.remove(i + 1);
        instructions.remove(i);
        if(Integer.parseInt(immediate.immediate) != 0){
            instructions.add(i, new Instruction(second.op, second.operand0, immediate));
        }

        String name = "Add immediate";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y++;
        this.removedMap.put(name, current);

        return true;
    }
    private boolean optimizeLoadFloat(LinkedList<Instruction> instructions, int i) {
        if (instructions.size() <= i + 2) {
            return false;
        }

        Instruction first = instructions.get(i);
        if (first.op != Operation.LEA || !first.operand0.isPrimary()) {
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(!second.op.isMove() || !second.operand0.isPrimaryFloat() || !second.operand1.isPrimaryEffective()){
            return false;
        }

        instructions.remove(i + 1);
        instructions.remove(i);
        instructions.add(i, new Instruction(second.op, second.operand0, first.operand1));
        String name = "Load Float";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y++;
        this.removedMap.put(name, current);

        return true;
    }
    private boolean optimizeStore(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 2){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.LEA || !first.operand0.isSecondary() || !first.operand1.isStackPointer()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.MOV || !second.operand0.isSecondaryEffective() || !second.operand1.isPrimary()){
            return false;
        }

        instructions.remove(i + 1);
        instructions.remove(i);

        instructions.add(i, new Instruction(Operation.MOV, first.operand1, second.operand1));

        String name = "Store";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y++;
        this.removedMap.put(name, current);

        return true;
    }
    private boolean optimizePointerArithmetic(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 3){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.MOV || !first.operand0.isPrimary() || !first.operand1.isImmediate()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.IMUL || !second.operand0.isPrimary() || !second.operand1.isImmediate()){
            return false;
        }
        Instruction third = instructions.get(i + 2);
        if(third.op != Operation.MOV || !third.operand0.isSecondary() || !third.operand1.isPrimary()){
            return false;
        }

        instructions.remove(i + 2);
        instructions.remove(i + 1);
        instructions.remove(i);
        Immediate fstImm = (Immediate) first.operand1;
        Immediate sndImm = (Immediate) second.operand1;

        int newImmValue = (Integer.parseInt(fstImm.immediate) * Integer.parseInt(sndImm.immediate));
        instructions.add(i, new Instruction(Operation.MOV, third.operand0, new Immediate(String.valueOf(newImmValue))));

        String name = "Pointer arithmetic";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y+=2;
        this.removedMap.put(name, current);

        return true;
    }
    private boolean optimizeSetupFloatingPointBinary(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 6){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.SUB || !first.operand0.isRSP() || !first.operand1.isImmediate()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(!second.op.isMove() || !second.operand0.isRSPEffective() || !second.operand1.isPrimaryFloat()){
            return false;
        }
        Instruction third = instructions.get(i + 2);
        if(!third.op.isMove()  || !third.operand0.isPrimaryFloat() || !third.operand1.isEffective()){
            return false;
        }
        Instruction fourth = instructions.get(i + 3);
        if(!fourth.op.isMove() || !fourth.operand0.isSecondaryFloat() || !second.operand1.isPrimaryFloat()){
            return false;
        }
        Instruction fifth = instructions.get(i + 4);
        if(!fifth.op.isMove() || !fifth.operand0.isPrimaryFloat() || !fifth.operand1.isRSPEffective()){
            return false;
        }
        Instruction sixth= instructions.get(i + 5);
        if(sixth.op != Operation.ADD || !sixth.operand0.isRSP() || !sixth.operand1.isImmediate()){
            return false;
        }

        instructions.remove(i + 5);
        instructions.remove(i + 4);
        instructions.remove(i + 3);
        instructions.remove(i + 2);
        instructions.remove(i + 1);
        instructions.remove(i);

        instructions.add(i, new Instruction(fifth.op, fourth.operand0, third.operand1));

        String name = "Setup floating point binary";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y+=4;
        this.removedMap.put(name, current);
        return true;
    }
    private boolean optimizeIndexing(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 3){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.MOV || !first.operand0.isSecondary() || !first.operand1.isImmediate()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.LEA || !second.operand0.isPrimary() || !second.operand1.isStackPointer()){
            return false;
        }
        Instruction third = instructions.get(i + 2);
        if(third.op != Operation.ADD || !third.operand0.isSecondary() || !third.operand1.isPrimary()){
            return false;
        }

        Register stackPointer = (Register) second.operand1;
        Immediate firstImm = (Immediate)first.operand1;
        int additionalOffset = Integer.parseInt(firstImm.immediate);
        instructions.remove(i + 2);
        instructions.remove(i + 1);
        instructions.remove(i);

        instructions.add(i, new Instruction(Operation.LEA, third.operand0, new Register(stackPointer.type, stackPointer.offset + additionalOffset, true)));

        String name = "Indexing";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y+=2;
        this.removedMap.put(name, current);

        return true;
    }
    private boolean optimizeSetupBinary(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 3){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.PUSH || !first.operand0.isPrimary()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(!(second.op == Operation.MOV || second.op == Operation.LEA) || !second.operand0.isSecondary()){
            return false;
        }
        Instruction third = instructions.get(i + 2);
        if(third.op != Operation.POP || !third.operand0.isPrimary()){
            return false;
        }
        instructions.remove(i + 2);
        instructions.remove(i);

        String name = "Setup Binary";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y+=2;
        this.removedMap.put(name, current);

        return true;
    }
    private boolean optimizeSignExtendImmediateMove(LinkedList<Instruction> instructions, int i) {
        if(instructions.size() <= i + 1){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.MOV || first.operand0.shouldBeExtendedPrimary() || !first.operand1.isImmediate()){
            return false;
        }

        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.MOVSX || !second.operand0.isPrimary() || second.operand1.shouldBeExtendedPrimary()){
            return false;
        }

        instructions.remove(i + 1);
        instructions.remove(i);
        instructions.add(i, new Instruction(Operation.MOV, second.operand0, first.operand1));

        String name = "Sign Extend Immediate";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y++;
        this.removedMap.put(name, current);

        return true;
    }

    private boolean optimizeSignExtend(LinkedList<Instruction> instructions, int i){
        if(instructions.size() <= i + 1){
            return false;
        }
        Instruction first = instructions.get(i);
        if(first.op != Operation.MOVSX || !first.operand0.isPrimary() || !first.operand1.isPrimary()){
            return false;
        }
        Instruction second = instructions.get(i + 1);
        if(second.op != Operation.MOV || !second.operand1.isPrimary() || !second.operand1.equals(first.operand1)){
            return false;
        }

        instructions.remove(i + 1);
        instructions.remove(i);
        instructions.add(i, new Instruction(Operation.MOV, second.operand0, first.operand1));

        String name = "Sign Extend";
        Point current = this.removedMap.getOrDefault(name, new Point(0, 0));
        current.x++;
        current.y++;
        this.removedMap.put(name, current);

        return true;
    }
}
