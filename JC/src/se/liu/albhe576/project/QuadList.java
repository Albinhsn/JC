package se.liu.albhe576.project;

import java.util.ArrayList;

public class QuadList extends ArrayList<Quad>{

    public Quad pop(){
       Quad out = this.getLastQuad();
       this.remove(this.size() - 1);
       return out;
    }

    public QuadOp getLastOp(){return this.getLastQuad().op;}
    public Quad getLastQuad(){return this.get(this.size() - 1);}
    public void addQuad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){this.add(new Quad(op, operand1, operand2, result));}
    public void addQuad(Quad quad){
        this.add(quad);
    }
    public Symbol getLastResult(){return this.getLastQuad().result;}
    public void removeLastQuad(){this.remove(this.size() - 1);}
    public Symbol getLastOperand1(){return this.getLastQuad().operand1;}
    public Symbol getLastOperand2(){return this.get(this.size() - 1).operand2;}
    public void insertLabel(Symbol label){this.add(new Quad(QuadOp.LABEL, label, null, null));}
    public Symbol createPop(Symbol toBePopped){
        Symbol popped = Compiler.generateSymbol(toBePopped.type);
        this.add(new Quad(QuadOp.POP, toBePopped, null, popped));
        return popped;
    }
    public void createPush(Symbol operandSymbol){this.add(new Quad(QuadOp.PUSH, operandSymbol, null, null));}

    public Symbol createSetupBinary(SymbolTable symbolTable, Syntax right, Symbol lSymbol) throws CompileException {

        this.createPush(lSymbol);
        right.compile(symbolTable, this);
        Symbol rSymbol = this.getLastResult();
        this.createMovRegisterAToC(rSymbol);
        this.createPop(Compiler.generateSymbol(lSymbol.type));

        return rSymbol;
    }

    public Symbol createSetupBinary(QuadList right, Symbol lSymbol, Symbol rSymbol) {
        this.createPush(lSymbol);
        this.addAll(right);
        this.createMovRegisterAToC(rSymbol);
        return this.createPop(lSymbol);

    }

    public Symbol createSetupUnary(SymbolTable symbolTable, Symbol result){

        int structSize = symbolTable.getStructSize(result.type);
        this.createPush(result);
        Symbol immSymbol = this.createLoadImmediate(DataType.getInt(), String.valueOf(structSize));
        this.createMovRegisterAToC(immSymbol);
        return this.createPop(result);
    }

    public Symbol createStore(Symbol symbol){
        Symbol out = Compiler.generateSymbol(symbol.type);
        this.addQuad(QuadOp.STORE, symbol, null, out);
        return out;
    }
    public Symbol createMovRegisterAToC(Symbol firstOperand){
        Symbol out = Compiler.generateSymbol(firstOperand.type);
        this.addQuad(QuadOp.MOV_REG_CA, firstOperand, null, out);
        return out;
    }
    public Symbol createLoadImmediate(DataType type, String immediate){
        Symbol out = Compiler.generateSymbol(type);
        this.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(type, immediate), null, out);
        return out;
    }
    public Symbol createConvertIntToFloat(Symbol toStore){
        Symbol newToStore = Compiler.generateSymbol(DataType.getFloat());
        this.addQuad(QuadOp.CVTSI2SD, toStore, null, newToStore);
        return newToStore;
    }
    public Symbol createConvertFloatToInt(Symbol toStore){
        Symbol newToStore = Compiler.generateSymbol(DataType.getInt());
        this.addQuad(QuadOp.CVTTSD2SI, toStore, null, newToStore);
        return newToStore;
    }

    public Symbol createLoadPointer(Symbol toLoad){
        Symbol loaded = Compiler.generateSymbol(toLoad.type);
        this.addQuad(QuadOp.LOAD_POINTER, toLoad, null, loaded);
        return loaded;
    }
    public Symbol createIndex(Symbol index, Symbol value) throws CompileException {
        Symbol result = Compiler.generateSymbol(value.type.getTypeFromPointer());
        this.addQuad(QuadOp.INDEX, index, value, result);
        return result;
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
        this.createPop(Compiler.generateSymbol(DataType.getInt()));
        this.addQuad(QuadOp.CMP, null, null,null);
    }

    public void insertJMPOnComparisonCheck(Symbol jmpLocation, boolean jumpIfTrue){
        this.insertBooleanComparison(jumpIfTrue ? "1" : "0");
        this.addQuad(QuadOp.JE, jmpLocation, null, null);
    }
    public void createStoreIndex(Symbol value, Symbol arr){this.addQuad(QuadOp.STORE_INDEX, value, arr, null);}
    public void createSetField(Symbol member, Symbol struct){this.addQuad(QuadOp.SET_FIELD, member, struct, null);}
    public Symbol createGetField(Symbol member, Symbol structSymbol){
        Symbol result = Compiler.generateSymbol(member.type);
        this.addQuad(QuadOp.GET_FIELD, structSymbol, member, result);
        return result;
    }
    public void createPushStruct(Symbol struct){this.addQuad(QuadOp.PUSH_STRUCT, struct, null, null);}
    public void createCall(Symbol functionSymbol, Symbol returnType){this.addQuad(QuadOp.CALL, functionSymbol, null, returnType);}
    public void createCmp(Symbol left, Symbol right){this.addQuad(QuadOp.CMP, left, right, null);}
    public void createJmp(Symbol label){this.addQuad(QuadOp.JMP, label, null, null);}
    public Symbol createLoad(Symbol symbol){
        Symbol loadedSymbol = Compiler.generateSymbol(symbol.type);
        this.addQuad(QuadOp.LOAD, symbol, null, loadedSymbol);
        return loadedSymbol;
    }
    public Symbol createIncrement(Symbol symbol){
        Symbol increased = Compiler.generateSymbol(symbol.type);
        this.addQuad(QuadOp.INC, symbol, null, increased);
        return increased;
    }
    public Symbol createDecrement(Symbol symbol){
        Symbol decreased= Compiler.generateSymbol(symbol.type);
        this.addQuad(QuadOp.DEC, symbol, null, decreased);
        return decreased;
    }
    public void createReturn(){this.addQuad(QuadOp.RET, null, null, null);}

}
