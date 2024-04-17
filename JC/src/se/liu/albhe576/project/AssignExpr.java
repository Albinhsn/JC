package se.liu.albhe576.project;

import java.util.List;

public class AssignExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s = %s",variable, value);
    }
    private final Expr variable;
    private final Expr value;

    public AssignExpr(Expr variable, Expr value, int line){
        super(line);
        this.variable = variable;
        this.value = value;
    }

    private QuadList compileStoreField(SymbolTable symbolTable, QuadList valueQuads, QuadList variableQuads) throws UnknownSymbolException {
        Quad lastQuad =  variableQuads.getLastQuad();
        variableQuads.removeLastQuad();
        Symbol struct = lastQuad.operand1;
        Symbol result = lastQuad.result;
        Symbol memberSymbol = symbolTable.getMemberSymbol(struct, lastQuad.operand2.name);


        Symbol pushed = Compiler.generateSymbol(result.type);
        variableQuads.addQuad(QuadOp.PUSH, result, null, pushed);
        variableQuads.concat(valueQuads);
        Symbol valResult = valueQuads.getLastResult();

        if(memberSymbol.type.type != DataTypes.FLOAT){
            variableQuads.addQuad(QuadOp.MOV_REG_CA, valResult, null, Compiler.generateSymbol(valResult.type));
        }

        Symbol popped = Compiler.generateSymbol(result.type);
        variableQuads.addQuad(QuadOp.POP, pushed, null, popped);
        variableQuads.addQuad(QuadOp.SET_FIELD, popped, memberSymbol, struct);

        return variableQuads;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {
        QuadList valueQuads = value.compile(symbolTable);
        QuadList variableQuads = variable.compile(symbolTable);


        // There is a difference when assinging a value whose location is on the stack
        // versus something that's accessed through a pointer to something
        // This is just looking essentially if the last thing in the variable is a get field
        if(variableQuads.getLastOp() == QuadOp.GET_FIELD){
            return this.compileStoreField(symbolTable, valueQuads, variableQuads);

        }

        // Figure out if legal?
        Symbol res = valueQuads.getLastResult();

        valueQuads.concat(variableQuads);
        valueQuads.addQuad(QuadOp.STORE, res, null, variableQuads.getLastResult());

        return valueQuads;
    }
}
