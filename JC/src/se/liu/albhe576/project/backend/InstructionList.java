package se.liu.albhe576.project.backend;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Container class for instructions
 * Most of the functionality ought to be defined within its subclass since it's based very much on the implementation
 * <p>
 * @see InstructionList
 * @see IntelInstructionList
 */

public class InstructionList implements Iterable<Instruction>{
    protected final List<Instruction> instructions;
    public InstructionList(){
	this.instructions = new ArrayList<>();
    }
    public int size(){
	return this.instructions.size();
    }

    public Iterator<Instruction> iterator() {
	return this.instructions.iterator();
    }
}
