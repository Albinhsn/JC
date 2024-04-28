package se.liu.albhe576.project;

public class AssignStmt extends Stmt{
    private final Expr variable;
    private final Expr value;

    public AssignStmt(Expr variable, Expr value, int line, String file){
        super(line, file);
        this.variable = variable;
        this.value = value;
    }

    public static Symbol convertValue(Symbol value, Symbol target, QuadList quads) throws CompileException {

        if(value.type.isSameType(target.type)){
            return value;
        }
        if(value.type.isPointer()){
            Quad lastQuad = quads.pop();
            quads.addQuad(lastQuad.op(), lastQuad.operand1(), lastQuad.operand2(), target);
            return quads.getLastResult();
        }

        switch(value.type.type){
            case DOUBLE -> {return  quads.createConvertDouble(value, target);}
            case FLOAT-> {return  quads.createConvertFloat(value,target);}
            case LONG -> {return  quads.createConvertLong(value,target);}
            case INT-> {return  quads.createConvertInt(value,target);}
            case SHORT-> {return  quads.createConvertShort(value,target);}
            case BYTE-> {return  quads.createConvertByte(value, target);}
        }
        throw new CompileException(String.format("Can't convert %s to %s", value.type.name, target.type.name));
    }

    public static boolean isInvalidAssignment(Symbol variableType, Symbol valueResult, Symbol lastOperand){
        return !(variableType.type.canBeCastedTo(valueResult.type)  || lastOperand.isNull());
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        value.compile(symbolTable, quads);

        QuadList variableQuads = new QuadList();
        variable.compile(symbolTable, variableQuads);

        Symbol valueResult = quads.getLastResult();

        Symbol variableType = variableQuads.getLastResult();
        Symbol variablePointer = variableQuads.getLastOperand1();
        variableQuads.pop();
        if(isInvalidAssignment(variableType, valueResult, quads.getLastOperand1())){
            Compiler.error(String.format("Trying to assign type %s to %s", valueResult.type, variableType.type), line, file);
        }

        valueResult = AssignStmt.convertValue(valueResult, variableType, quads);
        quads.createSetupBinary(variableQuads, valueResult, variablePointer);
        quads.createStoreVariable(variableType);
    }
}
