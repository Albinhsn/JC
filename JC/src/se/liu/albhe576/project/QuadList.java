package se.liu.albhe576.project;

import java.util.ArrayList;

public class QuadList extends ArrayList<Quad>{

    public Quad pop(){
       Quad out = this.getLastQuad();
       this.remove(this.size() - 1);
       return out;
    }

    public QuadOp getLastOp(){return this.getLastQuad().op();}
    public Quad getLastQuad(){return this.get(this.size() - 1);}
    public void addQuad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){this.add(new Quad(op, operand1, operand2, result));}
    public Symbol getLastResult(){return this.getLastQuad().result();}
    public Symbol getLastOperand1(){return this.getLastQuad().operand1();}
    public void insertLabel(Symbol label){this.add(new Quad(QuadOp.LABEL, label, null, null));}
    public Symbol createPop(Symbol toBePopped){
        Symbol popped = Compiler.generateSymbol(toBePopped.type);
        this.add(new Quad(QuadOp.POP, toBePopped, null, popped));
        return popped;
    }
    public void createPush(Symbol operandSymbol){this.add(new Quad(QuadOp.PUSH, operandSymbol, null, Compiler.generateSymbol(operandSymbol.type)));}
    public Symbol createSetupBinary(QuadList right, Symbol lSymbol, Symbol rSymbol) {
        this.createPush(lSymbol);
        this.addAll(right);
        this.createMovRegisterAToC(rSymbol);
        return this.createPop(lSymbol);
    }
    public void createStoreVariable(Symbol symbol){this.addQuad(QuadOp.STORE, symbol, null, Compiler.generateSymbol(symbol.type));}
    public void createMovRegisterAToC(Symbol firstOperand){this.addQuad(QuadOp.MOV_REG_CA, firstOperand, null, Compiler.generateSymbol(firstOperand.type));}
    public Symbol createLoadImmediate(DataType type, String immediate){
        Symbol out = Compiler.generateImmediateSymbol(type, immediate);
        this.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(type, immediate), null,out);
        return out;
    }
    public void createIMUL(int immediate){this.addQuad(QuadOp.IMUL , Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(immediate)), null,Compiler.generateSymbol(DataType.getInt()));}
    public Symbol createConvertByte(Symbol value, Symbol target){
        Symbol extended = Compiler.generateSymbol(target.type);
        this.addQuad(QuadOp.SIGN_EXTEND_BYTE, value, null, extended);
        if(target.type.isFloat()) {
            this.addQuad(QuadOp.CONVERT_INT_TO_FLOAT, extended, null, Compiler.generateSymbol(DataType.getFloat()));
        }
        else if(target.type.isDouble()){
            this.addQuad(QuadOp.CONVERT_LONG_TO_DOUBLE, this.getLastResult(), null, Compiler.generateSymbol(DataType.getDouble()));
        }
        return this.getLastResult();
    }
    public Symbol createConvertInt(Symbol value, Symbol target) throws CompileException {
        switch(target.type.type){
            case DOUBLE -> {
                this.addQuad(QuadOp.CONVERT_LONG_TO_DOUBLE, value, null, Compiler.generateSymbol(DataType.getDouble()));
                return this.getLastResult();
            }
            case BYTE -> {
                this.addQuad(QuadOp.SIGN_EXTEND_BYTE, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case SHORT -> {
                this.addQuad(QuadOp.SIGN_EXTEND_SHORT, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case FLOAT -> {
                this.addQuad(QuadOp.CONVERT_INT_TO_FLOAT, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case LONG -> {
                this.addQuad(QuadOp.SIGN_EXTEND_INT, null, null, Compiler.generateSymbol(DataType.getLong()));
                return this.getLastResult();
            }
            case STRUCT -> {
                if(value.isNull()){
                    return this.getLastResult();
                }
            }
        }
        throw new CompileException(String.format("Can't convert int to %s", target.type.name));
    }
    public Symbol createConvertFloat(Symbol value, Symbol target) throws CompileException {
        switch(target.type.type){
            case DOUBLE -> {
                this.addQuad(QuadOp.CONVERT_FLOAT_TO_DOUBLE, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case BYTE -> {
                Symbol intSymbol = Compiler.generateSymbol(DataType.getInt());
                this.addQuad(QuadOp.CONVERT_FLOAT_TO_INT, value, null, intSymbol);
                this.addQuad(QuadOp.SIGN_EXTEND_BYTE, intSymbol, null, Compiler.generateSymbol(DataType.getByte()));
                return this.getLastResult();
            }
            case SHORT -> {
                Symbol intSymbol = Compiler.generateSymbol(DataType.getInt());
                this.addQuad(QuadOp.CONVERT_FLOAT_TO_INT, value, null, intSymbol);
                this.addQuad(QuadOp.SIGN_EXTEND_SHORT, intSymbol, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case INT -> {
                this.addQuad(QuadOp.CONVERT_FLOAT_TO_INT, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case LONG -> {
                this.addQuad(QuadOp.CONVERT_FLOAT_TO_INT, value, null, Compiler.generateSymbol(DataType.getLong()));
                return this.getLastResult();
            }
        }
        throw new CompileException(String.format("Can't convert float to %s", target.type.name));
    }
    public Symbol createConvertLong(Symbol value, Symbol target) throws CompileException {
        switch(target.type.type){
            case BYTE -> {
                this.addQuad(QuadOp.SIGN_EXTEND_BYTE, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case SHORT -> {
                this.addQuad(QuadOp.SIGN_EXTEND_SHORT, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case INT -> {
                this.addQuad(QuadOp.SIGN_EXTEND_INT, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case FLOAT -> {
                this.addQuad(QuadOp.CONVERT_LONG_TO_DOUBLE, value, null, Compiler.generateSymbol(DataType.getDouble()));
                this.addQuad(QuadOp.CONVERT_DOUBLE_TO_FLOAT, this.getLastResult(), null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case DOUBLE -> {
                this.addQuad(QuadOp.CONVERT_LONG_TO_DOUBLE, value, null, Compiler.generateSymbol(DataType.getDouble()));
                return this.getLastResult();
            }
        }
        throw new CompileException(String.format("Can't convert long to %s", target.type));
    }
    public Symbol createConvertShort(Symbol value, Symbol target) throws CompileException {
        switch(target.type.type){
            case BYTE -> {
                this.addQuad(QuadOp.SIGN_EXTEND_BYTE, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case INT, LONG -> {
                this.addQuad(QuadOp.SIGN_EXTEND_SHORT, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case FLOAT -> {
                Symbol intSymbol = Compiler.generateSymbol(DataType.getInt());
                this.addQuad(QuadOp.SIGN_EXTEND_SHORT, value, null, intSymbol);
                this.addQuad(QuadOp.CONVERT_INT_TO_FLOAT, intSymbol, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case DOUBLE -> {
                Symbol longSymbol = Compiler.generateSymbol(DataType.getLong());
                this.addQuad(QuadOp.SIGN_EXTEND_SHORT, value, null, longSymbol);
                this.addQuad(QuadOp.CONVERT_LONG_TO_DOUBLE, longSymbol, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
        }
        throw new CompileException(String.format("Can't convert short to %s", target.type.name));
    }
    public Symbol createConvertDouble(Symbol value, Symbol target) throws CompileException {
        switch(target.type.type){
            case BYTE -> {
                Symbol longSymbol = Compiler.generateSymbol(DataType.getLong());
                this.addQuad(QuadOp.CONVERT_DOUBLE_TO_LONG, value, null, longSymbol);
                this.addQuad(QuadOp.SIGN_EXTEND_BYTE, longSymbol, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case SHORT -> {
                Symbol longSymbol = Compiler.generateSymbol(DataType.getLong());
                this.addQuad(QuadOp.CONVERT_DOUBLE_TO_LONG, value, null, longSymbol);
                this.addQuad(QuadOp.SIGN_EXTEND_SHORT, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case INT -> {
                Symbol longSymbol = Compiler.generateSymbol(DataType.getLong());
                this.addQuad(QuadOp.CONVERT_DOUBLE_TO_LONG, value, null, longSymbol);
                this.addQuad(QuadOp.SIGN_EXTEND_INT, value, null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case FLOAT -> {
                this.addQuad(QuadOp.CONVERT_DOUBLE_TO_FLOAT, this.getLastResult(), null, Compiler.generateSymbol(target.type));
                return this.getLastResult();
            }
            case LONG -> {
                this.addQuad(QuadOp.CONVERT_DOUBLE_TO_LONG, value, null, Compiler.generateSymbol(DataType.getLong()));
                return this.getLastResult();
            }
        }
        throw new CompileException(String.format("Can't convert short to %s", target.type.name));
    }

    public Symbol createAdd(Symbol left, Symbol right){
        Symbol result = Compiler.generateSymbol(left.type);
        this.addQuad(QuadOp.ADD, left, right, result);
        return result;
    }

    public void insertBooleanComparison(String immediateLiteral){
        Symbol immLoadResult = Compiler.generateSymbol(DataType.getInt());
        this.createPush(immLoadResult);
        this.createLoadImmediate(DataType.getInt(), immediateLiteral);
        this.createMovRegisterAToC(immLoadResult);
        Symbol popped = this.createPop(Compiler.generateSymbol(DataType.getInt()));
        this.addQuad(QuadOp.CMP, popped, null,null);
    }

    public void insertJMPOnComparisonCheck(Symbol jmpLocation, boolean jumpIfTrue){
        this.insertBooleanComparison(jumpIfTrue ? "1" : "0");
        this.addQuad(QuadOp.JE, jmpLocation, null, null);
    }
    public void createStore(Symbol value, Symbol arr){this.addQuad(QuadOp.STORE, value, arr, value);}
    public void createLoad(Symbol pointer){this.addQuad(QuadOp.LOAD, pointer, null, Compiler.generateSymbol(pointer.type.getTypeFromPointer()));}
    public void createJmpOnCondition(QuadOp condition, Symbol label){this.addQuad(condition, label, null, null);}
    public void allocateArguments(int size){this.addQuad(QuadOp.ALLOCATE, new ImmediateSymbol("size", DataType.getInt(), String.valueOf(size)), null, null);}
    public void createMoveArgument(Symbol argSymbol, int offset){this.addQuad(QuadOp.MOVE_ARG, argSymbol, new ImmediateSymbol("size", DataType.getInt(), String.valueOf(offset)), null);}

    public void createJumpOnComparison(Symbol label, boolean inverted) throws CompileException {
        QuadOp conditionOp = this.getLastOp();
        if(conditionOp.isSet()){
            this.pop();
            QuadOp jmpCondition = conditionOp.getJmpFromSet();
            if(inverted){
                jmpCondition = jmpCondition.invertJmpCondition();
            }
            this.createJmpOnCondition(jmpCondition, label);
        }else{
            this.insertJMPOnComparisonCheck(label, !inverted);
        }
    }
    public static QuadListPair compileBinary(SymbolTable symbolTable, QuadList quads, Expr left, Expr right) throws CompileException {
        left.compile(symbolTable, quads);
        QuadList rQuads = new QuadList();
        right.compile(symbolTable, rQuads);
        return new QuadListPair(quads, rQuads);
    }
}
