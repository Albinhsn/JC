package se.liu.albhe576.project;

public class AssignExpr extends Expr{
    private final Expr variable;
    private final Expr value;

    public AssignExpr(Expr variable, Expr value, int line, String file){
        super(line, file);
        this.variable = variable;
        this.value = value;
    }

    private static Symbol convertValue(Symbol value, Symbol target, QuadList quads){
        if(value.type.isFloatingPoint() && !target.type.isFloatingPoint()){
            value = quads.createConvertFloatToInt(value);
        }
        else if(target.type.isFloatingPoint() && !value.type.isFloatingPoint()){
            value = quads.createConvertIntToFloat(value);
        }
        return value;
    }

    private static void compileStoreField(SymbolTable symbolTable, QuadList quads, QuadList variableQuads) throws CompileException {
        // Last op will be a GET_FIELD, which means it will not have the pointer to the struct in rax
        // So we have to remove the op
        Quad lastQuad =  variableQuads.pop();

        Symbol struct = lastQuad.getOperand1();
        Symbol memberSymbol = symbolTable.getMemberSymbol(lastQuad.getOperand1(), lastQuad.getOperand2().name);
        Symbol value = quads.getLastResult();

        value = convertValue(value, memberSymbol, quads);

        quads.createPush(value);
        quads.addAll(variableQuads);
        Symbol varResult = quads.getLastResult();

        quads.createMovRegisterAToC(varResult);

        quads.createPop(value);
        quads.createSetField(memberSymbol, struct);
    }

    private static void compileStoreDereference(QuadList valueQuads, QuadList variableQuads) {
        Symbol dereferenced = variableQuads.getLastOperand1();
        Symbol variableType = variableQuads.getLastResult();

        Symbol valueResult = valueQuads.getLastResult();
        variableQuads.removeLastQuad();

        valueResult = convertValue(valueResult, variableType, valueQuads);

        valueQuads.createSetupBinary(variableQuads, valueResult, dereferenced);
        valueQuads.createStoreIndex(valueResult, dereferenced);
    }

    private static void compileStoreIndex(QuadList quads, QuadList variableQuads) throws CompileException{
        Symbol res = variableQuads.getLastOperand2();
        Symbol toStore = quads.getLastResult();

        quads.createPush(toStore);

        DataType resType = res.type.getTypeFromPointer();
        quads.addAll(variableQuads);

        // When storing to an index the index expression doesn't know whether or not it's an assignment
        // which means it will end with INDEX, so we need to remove it and leave the pointer
        // Otherwise the value we want to store to ends up in rax and not a pointer to it
        quads.removeLastQuad();

        quads.createMovRegisterAToC(quads.getLastResult());
        quads.createPop(toStore);

        Symbol result = Compiler.generateSymbol(resType);
        toStore = convertValue(toStore, result, quads);

        quads.createStoreIndex(result, toStore);
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        value.compile(symbolTable, quads);


        QuadList variableQuads = new QuadList();
        variable.compile(symbolTable, variableQuads);

        Symbol valueResult = quads.getLastResult();
        QuadOp lastOp = variableQuads.getLastOp();
        Symbol variableType = variableQuads.getLastResult();


        // Type check the assignment
        boolean same = variableType.type.isSameType(valueResult.type);
        boolean canBeCasted = variableType.type.canBeCastedTo(valueResult.type);
        boolean isNull = quads.getLastOperand1().isNull();
        if(!(same || canBeCasted || isNull)){
            this.error(String.format("Trying to assign type %s to %s", valueResult.type.name, variableType.type.name));
        }


        if(lastOp == QuadOp.GET_FIELD){
            // We store into a struct
            compileStoreField(symbolTable, quads, variableQuads);

        }else if(lastOp == QuadOp.DEREFERENCE){
            // Or where a pointer points to
            compileStoreDereference(quads, variableQuads);
        }
        else if(lastOp == QuadOp.INDEX){
            // Or an index
            compileStoreIndex(quads, variableQuads);
        }
        else if (valueResult.type.isStruct()){

            // Storing a struct takes a pointer to the location in rcx
            // And a pointer to the value in rax
            // Then some hack to move everything (depends on the size) is handled in Stack
            quads.createSetupBinary(variableQuads, valueResult, variableType);
            quads.addQuad(QuadOp.MOVE_STRUCT, valueResult, variableType, null);
        }else{

            // Do we need to case the value from float to int or other way around
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
