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
    public void removeLastQuad(){this.remove(this.size() - 1);}
    public Symbol getLastOperand1(){return this.getLastQuad().operand1();}
    public void insertLabel(Symbol label){this.add(new Quad(QuadOp.LABEL, label, null, null));}
    public Symbol createPop(Symbol toBePopped){
        Symbol popped = Compiler.generateSymbol(toBePopped.type);
        this.add(new Quad(QuadOp.POP, toBePopped, null, popped));
        return popped;
    }
    public void createPush(Symbol operandSymbol){this.add(new Quad(QuadOp.PUSH, operandSymbol, null, Compiler.generateSymbol(operandSymbol.type)));}
    public Symbol createSetupBinary(QuadList right, Symbol lSymbol, Symbol rSymbol, Symbol target) {
        this.createPush(lSymbol);
        this.addAll(right);
        this.createMovRegisterAToC(rSymbol);
        lSymbol = this.createPop(lSymbol);
        return AssignStmt.convertValue(lSymbol, target, this);
    }
    public void createSetupBinary(QuadList right, Symbol lSymbol, Symbol rSymbol) {createSetupBinary(right, lSymbol, rSymbol, rSymbol);}
    public void createStore(Symbol symbol){this.addQuad(QuadOp.STORE, symbol, null, Compiler.generateSymbol(symbol.type));}
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
    public void createIMUL(int immediate){this.addQuad(QuadOp.IMUL , Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(immediate)), null,Compiler.generateSymbol(DataType.getInt()));}
    public Symbol createConvertByteToInt(Symbol toStore){
        Symbol newToStore = Compiler.generateSymbol(DataType.getInt());
        this.addQuad(QuadOp.CONVERT_BYTE_TO_INT, toStore, null, newToStore);
        return newToStore;
    }
    public Symbol createConvertIntToFloat(Symbol toStore){
        Symbol newToStore = Compiler.generateSymbol(DataType.getFloat());
        this.addQuad(QuadOp.CONVERT_INT_TO_FLOAT, toStore, null, newToStore);
        return newToStore;
    }
    public Symbol createConvertFloatToInt(Symbol toStore){
        Symbol newToStore = Compiler.generateSymbol(DataType.getInt());
        this.addQuad(QuadOp.CONVERT_FLOAT_TO_INT, toStore, null, newToStore);
        return newToStore;
    }

    public Symbol createLoadPointer(Symbol toLoad){
        Symbol loaded = Compiler.generateSymbol(toLoad.type);
        this.addQuad(QuadOp.LOAD_VARIABLE_POINTER, toLoad, null, loaded);
        return loaded;
    }
    public void createIndex(Symbol value){this.addQuad(QuadOp.INDEX, null, value, Compiler.generateSymbol(value.type.getTypeFromPointer()));}
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
    public void createStoreIndex(Symbol value, Symbol arr){this.addQuad(QuadOp.STORE_INDEX, value, arr, value);}
    public void createSetField(Symbol member, Symbol struct){this.addQuad(QuadOp.SET_FIELD, member, struct, member);}
    public void createGetField(Symbol member, Symbol structSymbol){this.addQuad(QuadOp.GET_FIELD, structSymbol, member,Compiler.generateSymbol(member.type));}
    public void createCall(Symbol functionSymbol, Symbol returnType){this.addQuad(QuadOp.CALL, functionSymbol, null, returnType);}
    public void createCmp(Symbol left, Symbol right){this.addQuad(QuadOp.CMP, left, right, null);}
    public void createJmp(Symbol label){this.addQuad(QuadOp.JMP, label, null, null);}
    public void createJmp(QuadOp condition, Symbol label){this.addQuad(condition, label, null, null);}
    public void createIncrement(Symbol symbol){this.addQuad(QuadOp.INC, symbol, null, Compiler.generateSymbol(symbol.type));}
    public void createDecrement(Symbol symbol){this.addQuad(QuadOp.DEC, symbol, null, Compiler.generateSymbol(symbol.type));}
    public void createReturn(){this.addQuad(QuadOp.RET, null, null, null);}
    public void allocateArguments(int size){this.addQuad(QuadOp.ALLOCATE, new ImmediateSymbol("size", DataType.getInt(), String.valueOf(size)), null, null);}
    public void createMoveArgument(Symbol argSymbol, int offset){this.addQuad(QuadOp.MOVE_ARG, argSymbol, new ImmediateSymbol("size", DataType.getInt(), String.valueOf(offset)), null);}

    public void createJumpOnComparison(Symbol label, boolean inverted) throws CompileException {
        QuadOp conditionOp = this.getLastOp();
        if(conditionOp.isSet()){
            this.removeLastQuad();
            QuadOp jmpCondition = conditionOp.getJmpFromSet();
            if(inverted){
                jmpCondition = jmpCondition.invertJmpCondition();
            }
            this.createJmp(jmpCondition, label);
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
    public Symbol createSetupPointerOp(Symbol pointerSymbol, int immediate){
        Symbol loadedImmediate = this.createLoadImmediate(DataType.getInt(), String.valueOf(immediate));
        this.createMovRegisterAToC(loadedImmediate);
        Symbol loadedPointer = this.createLoadPointer(pointerSymbol);
        return this.createAdd(loadedPointer, Compiler.generateSymbol(DataType.getInt()));
    }

}
