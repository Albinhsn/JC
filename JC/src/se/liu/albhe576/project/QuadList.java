package se.liu.albhe576.project;

import java.util.ArrayList;

public class QuadList extends ArrayList<Quad>{

    public QuadOp getLastOp(){return this.getLastQuad().op;}
    public Quad getLastQuad(){return this.get(this.size() - 1);}
    public void addQuad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){
        this.add(new Quad(op, operand1, operand2, result));
    }
    public void addQuad(Quad quad){
        this.add(quad);
    }

    public Symbol getLastResult(){return this.getLastQuad().result;}
    public void removeLastQuad(){this.remove(this.size() - 1);}
    public Symbol getLastOperand1(){return this.getLastQuad().operand1;}
    public Symbol getLastOperand2(){return this.get(this.size() - 1).operand2;}
    public void insertLabel(Symbol label){this.add(new Quad(QuadOp.LABEL, label, null, null));}
    public void createPop(Symbol resultSymbol){
        this.add(new Quad(QuadOp.POP, null, null, resultSymbol));
    }
    public void createPush(Symbol operandSymbol){this.add(new Quad(QuadOp.PUSH, operandSymbol, null, null));}

    public Symbol createSetupBinary(SymbolTable symbolTable, Syntax right, Symbol lSymbol) throws UnexpectedTokenException, UnknownSymbolException, CompileException, InvalidOperation {

        this.createPush(lSymbol);
        right.compile(symbolTable, this);
        Symbol rSymbol = this.getLastResult();
        this.addQuad(QuadOp.MOV_REG_CA, rSymbol, null, rSymbol);
        this.createPop(Compiler.generateSymbol(lSymbol.type));

        return rSymbol;
    }

    public void createSetupBinary(QuadList right, Symbol lSymbol, Symbol rSymbol) {
        this.createPush(lSymbol);
        this.addAll(right);
        this.addQuad(QuadOp.MOV_REG_CA, rSymbol, null, rSymbol);
        this.createPop(Compiler.generateSymbol(lSymbol.type));

    }

    public void createSetupUnary(SymbolTable symbolTable, Symbol result){

        int structSize = symbolTable.getStructSize(result.type);
        this.createPush(result);
        Symbol immSymbol = Compiler.generateSymbol(DataType.getInt());
        this.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(structSize)), null, immSymbol);
        Symbol movedImm = Compiler.generateSymbol(DataType.getInt());
        this.addQuad(QuadOp.MOV_REG_CA, immSymbol, null, movedImm);
        this.createPop(Compiler.generateSymbol(result.type));
    }

}
