package se.liu.albhe576.project;

public class AssignExpr extends Expr{
    private final Expr variable;
    private final Expr value;

    public AssignExpr(Expr variable, Expr value, int line, String file){
        super(line, file);
        this.variable = variable;
        this.value = value;
    }

    private void compileStoreField(SymbolTable symbolTable, QuadList quads, QuadList variableQuads) throws CompileException {
        Quad lastQuad =  variableQuads.pop();

        Symbol struct = lastQuad.operand1;
        Symbol memberSymbol = symbolTable.getMemberSymbol(lastQuad.operand1, lastQuad.operand2.name);
        Symbol value = quads.getLastResult();

        if(value.type.isFloatingPoint() && !memberSymbol.type.isFloatingPoint()){
             value = quads.createConvertFloatToInt(value);
        }
        else if(memberSymbol.type.isFloatingPoint() && !value.type.isFloatingPoint()){
            value = quads.createConvertIntToFloat(value);
        }

        quads.createPush(value);
        quads.addAll(variableQuads);
        Symbol varResult = quads.getLastResult();

        if(!memberSymbol.type.isFloatingPoint()){
            quads.createMovRegisterAToC(varResult);
        }

        quads.createPop(value);
        quads.createSetField(memberSymbol, struct);
    }

    private void compileStoreDereference(QuadList valueQuads, QuadList variableQuads) {
        Symbol dereferenced = variableQuads.getLastOperand1();
        Symbol valueResult = valueQuads.getLastResult();
        Symbol variableType = variableQuads.getLastResult();
        variableQuads.removeLastQuad();

        if(valueResult.type.isFloatingPoint() && !variableType.type.isFloatingPoint()){
            valueResult = valueQuads.createConvertFloatToInt(valueResult);
        }
        else if(!valueResult.type.isFloatingPoint() && variableType.type.isFloatingPoint()){
            valueResult = valueQuads.createConvertIntToFloat(valueResult);
        }

        valueQuads.createSetupBinary(variableQuads, valueResult, dereferenced);
        valueQuads.createStoreIndex(valueResult, dereferenced);
    }

    private void compileStoreIndex(QuadList quads, QuadList variableQuads) throws CompileException{
        Symbol res = variableQuads.getLastOperand2();
        Symbol toStore = quads.getLastResult();

        quads.createPush(toStore);

        DataType resType = res.type.getTypeFromPointer();
        quads.addAll(variableQuads);
        quads.removeLastQuad();
        quads.createMovRegisterAToC(quads.getLastResult());
        // ToDo check if they can be converted
        if(!resType.isSameType(toStore.type)){
            quads.createPop(toStore);
            if(resType.isFloatingPoint()){
                toStore = quads.createConvertIntToFloat(toStore);
            }else if(toStore.type.isFloatingPoint()){
                toStore = quads.createConvertFloatToInt(toStore);
            }else{
                this.error("What are you trying to do?");
            }
        }else{
            quads.createPop(toStore);
        }

        quads.createStoreIndex(Compiler.generateSymbol(res.type.getTypeFromPointer()), toStore);
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        value.compile(symbolTable, quads);


        QuadList variableQuads = new QuadList();
        variable.compile(symbolTable, variableQuads);

        Symbol valueResult = quads.getLastResult();
        QuadOp lastOp = variableQuads.getLastOp();
        Symbol variableType = variableQuads.getLastResult();


        if(!variableType.type.isSameType(valueResult.type) && !variableType.type.canBeCastedTo(valueResult.type)){
            this.error(String.format("Trying to assign type %s to %s", valueResult.type.name, variableType.type.name));
        }


        if(lastOp == QuadOp.GET_FIELD){
            this.compileStoreField(symbolTable, quads, variableQuads);
        }else if(lastOp == QuadOp.DEREFERENCE){
            this.compileStoreDereference(quads, variableQuads);
        }
        else if(lastOp == QuadOp.INDEX){
            this.compileStoreIndex(quads, variableQuads);
        }
        else if (valueResult.type.isStruct()){
            quads.createSetupBinary(variableQuads, valueResult, variableType);
            quads.addQuad(QuadOp.MOVE_STRUCT, valueResult, variableType, null);
        }else{
            if(valueResult.type.isFloatingPoint() && !variableType.type.isFloatingPoint()){
                valueResult = quads.createConvertFloatToInt(valueResult);
            }
            else if(!valueResult.type.isFloatingPoint() && variableType.type.isFloatingPoint()){
                valueResult = quads.createConvertIntToFloat(valueResult);
            }
            quads.createSetupBinary(variableQuads, valueResult, variableType);
            quads.createStore(variableQuads.getLastOperand1());
        }


    }
}
