package se.liu.albhe576.project.frontend;

import java.util.List;
import se.liu.albhe576.project.backend.*;

/**
 * A collection of if blocks and and else block.
 * The usage of IfBlock allows for easier definition of "else if"
 * @see IfBlock
 * @see Statement
 */
public class IfStatement extends Statement
{

    private final List<IfBlock> ifBlocks;
    private final List<Statement> elseBody;
    public IfStatement(List<IfBlock> ifBlocks, List<Statement> elseBody, int line, String file){
        super(line, file);
        this.ifBlocks = ifBlocks;
        this.elseBody= elseBody;

    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws  CompileException{

        // Iterate over every ifBlock (this includes else if)
        // And if the condition is true we execute the block and we jump to the merge label
        // Or just jump to the next "else" label (which can be next else if)
        Symbol mergeLabel = symbolTable.generateLabel();
        for(IfBlock block : this.ifBlocks){
            block.condition().compile(symbolTable, intermediates);

            Symbol elseLabel = symbolTable.generateLabel();
            intermediates.createJumpCondition(elseLabel, false);

            Statement.compileBlock(symbolTable, intermediates, block.body());

            intermediates.createJump(mergeLabel);
            intermediates.insertLabel(elseLabel);
        }

        Statement.compileBlock(symbolTable, intermediates, elseBody);
        intermediates.insertLabel(mergeLabel);
    }
}
