package se.liu.albhe576.project.backend;


/**
 *
 * A record for intermediates which contains an operation, two optional operands and an optional result
 * @see IntermediateOperation
 * @see Symbol
 * @param op
 * @param operand1
 * @param operand2
 * @param result
 */
public record Intermediate(IntermediateOperation op, Symbol operand1, Symbol operand2, Symbol result) {
    @Override
    public String toString() {
        return String.format("%s %s %s %s",op.name(),operand1,operand2,result);
    }

    public static Symbol convertType(SymbolTable symbolTable, IntermediateList intermediates, Symbol symbol, DataType target){
        if(!symbol.type.isSameType(target)){
            symbol = intermediates.createConvert(symbolTable, symbol, target);
        }
        return symbol;
    }
}
