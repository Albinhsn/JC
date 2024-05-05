package se.liu.albhe576.project.frontend;

import java.util.List;
import se.liu.albhe576.project.backend.*;

/**
 * The statements are declared as while(condition)
 * The structure of this compiled is
 * label        - the header label
 * condition    - the condition from which if false will jump to the last label
 * body         -  the body of the function
 * update       - the update code for the loop, automatically jump back to the top label when reached
 * label        - the label merge label from which we exit the loop
 * @see Statement
 */
public class WhileStatement extends Statement
{
    private final Expression condition;
    private final List<Statement> body;
    public WhileStatement(Expression condition, List<Statement> body, int line, String file){
        super(line, file);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws CompileException {
        Symbol condLabel = symbolTable.generateLabel();
        intermediates.insertLabel(condLabel);
        condition.compile(symbolTable, intermediates);

        Symbol mergeLabel = symbolTable.generateLabel();
        intermediates.createJumpCondition(mergeLabel, false);

        symbolTable.enterScope();
        for(Statement statement : body){
            statement.compile(symbolTable, intermediates);
        }
        intermediates.createJump(condLabel);
        intermediates.insertLabel(mergeLabel);
        symbolTable.exitScope();
    }
}
