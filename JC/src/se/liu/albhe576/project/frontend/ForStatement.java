package se.liu.albhe576.project.frontend;

import java.util.List;
import se.liu.albhe576.project.backend.*;

/**
 * The statement for for loops
 * The statements are declared as for(init, condition, update)
 * The structure of this compiled is
 * init
 * label        - the header label
 * condition    - the condition from which if false will jump to the last label
 * body         -  the body of the function
 * update       - the update code for the loop, automatically jump back to the top label when reached
 * label        - the label merge label from which we exit the loop
 * @see Statement
 */
public class ForStatement extends Statement
{
    private final Statement init;
    private final Statement condition;
    private final Statement update;
    private final List<Statement> body;
    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws  CompileException{

        symbolTable.enterScope();
        init.compile(symbolTable, intermediates);

        Symbol conditionLabel = symbolTable.generateLabel();
        Symbol mergeLabel = symbolTable.generateLabel();

        // Compile condition
        intermediates.insertLabel(conditionLabel);
        IntermediateList conditionIntermediates = new IntermediateList();
        condition.compile(symbolTable, conditionIntermediates);

        intermediates.addAll(conditionIntermediates);
        if(!conditionIntermediates.isEmpty()){
            // check if we jump
            intermediates.createJumpCondition(mergeLabel, false);
        }


        // Compile body
        for(Statement statement : body){
            statement.compile(symbolTable, intermediates);
        }

        // Compile update and jumps
        update.compile(symbolTable, intermediates);
        intermediates.createJump(conditionLabel);
        intermediates.insertLabel(mergeLabel);
        symbolTable.exitScope();
    }

    public ForStatement(Statement init, Statement condition, Statement update, List<Statement> body, int line, String file){
        super(line, file);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }
}
