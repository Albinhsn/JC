package se.liu.albhe576.project;

import java.util.ArrayList;

public class QuadList extends ArrayList<Quad>{

    public Quad getLastQuad(){return this.get(this.size() - 1);}
    public Symbol getLastOperand1(){return this.getLastQuad().operand1();}
    public Symbol getLastResult(){return this.getLastQuad().result();}
    public void insertLabel(Symbol label){this.add(new Quad(QuadOp.LABEL, label, null, null));}
    public void createReturn(Symbol toBeReturned){
        QuadOp op = toBeReturned != null && toBeReturned.type.isFloat() ? QuadOp.RET_F : QuadOp.RET_I;
        this.add(new Quad(op, null, null, toBeReturned));
    }
    public void createCall(Symbol function){
        this.add(new Quad(QuadOp.CALL, function, null, Compiler.generateSymbol(function.type)));
    }
    public void createReference(Symbol source){
        this.add(new Quad(QuadOp.REFERENCE, source, null, Compiler.generateSymbol(DataType.getPointerFromType(source.type))));
    }
    public void createDereference(Symbol source) throws CompileException {
        this.add(new Quad(QuadOp.DEREFERENCE, source, null, Compiler.generateSymbol(source.type.getTypeFromPointer())));
    }
    public void createNegate(Symbol source){
        this.add(new Quad(QuadOp.NEGATE, source, null, Compiler.generateSymbol(source.type)));
    }
    public Symbol createConvert(Symbol source, DataType target){
        Symbol converted = Compiler.generateSymbol(target);
        this.add(new Quad(QuadOp.CONVERT, source, null, converted));
        return converted;
    }
    public void createJumpCondition(Symbol label, boolean jumpIfTrue){
        QuadOp op = jumpIfTrue ? QuadOp.JMP_T : QuadOp.JMP_F;
        this.add(new Quad(op, label, null, null));
    }
    public void createPostfix(Symbol target, TokenType op) throws CompileException {
        QuadOp quadOp = QuadOp.fromToken(op);
        if(target.type.isFloatingPoint()){
            quadOp = quadOp.convertToFloat();
        }
        this.add(new Quad(quadOp, target, null, target));

    }
    public void createJump(Symbol label){
        this.add(new Quad(QuadOp.JMP, label, null, null));
    }
    public void createLoadImmediate(ImmediateSymbol immediate){
        QuadOp op = immediate.type.isInteger() ? QuadOp.LOAD_IMM_I : QuadOp.LOAD_IMM_F;
        this.add(new Quad(op, immediate, null, Compiler.generateSymbol(immediate.type)));
    }
    public void createMember(Symbol source, Symbol member){
        this.add(new Quad(QuadOp.MEMBER, source, member, Compiler.generateSymbol(member.type)));
    }
    public void createIndex(Symbol value, Symbol index) throws CompileException {
        this.add(new Quad(QuadOp.INDEX, value, index, Compiler.generateSymbol(value.type.getTypeFromPointer())));
    }

    public void createStore(Symbol value, Symbol variable){
        QuadOp op = variable.type.isFloat() ? QuadOp.STORE_F : QuadOp.STORE_I;
        this.add(new Quad(op, value, null, variable));
    }

    public void createBinaryOp(QuadOp op, Symbol left, Symbol right, DataType result) {
        this.add(new Quad(op, left, right, Compiler.generateSymbol(result)));
    }
    public void createParam(Symbol param){
        this.add(new Quad(QuadOp.PARAM, param, null, null));
    }
    public void createCast(Symbol value, DataType target){
        if(value.type.isSameType(target)){
            return;
        }
        this.add(new Quad(QuadOp.CONVERT, value, null, Compiler.generateSymbol(target)));
    }

    public void createComparison(TokenType op, Symbol left, Symbol right) throws CompileException {
        this.add(new Quad(QuadOp.fromToken(op), left, right, Compiler.generateSymbol(DataType.getInt())));
    }

    public void createLoad(VariableSymbol symbol){
        QuadOp op;
        if(symbol.type.isPointer()){
            op = QuadOp.LOAD_POINTER;
        }else if(symbol.type.isInteger()){
            op = QuadOp.LOAD_I;
        }else{
            op = QuadOp.LOAD_F;
        }
        Symbol loaded = Compiler.generateSymbol(symbol.type);
        this.add(new Quad(op, symbol, null, loaded));
    }
    public void createLogical(Symbol left, Symbol right, TokenType op){
        QuadOp quadOp =  op == TokenType.TOKEN_AND_LOGICAL ? QuadOp.LOGICAL_AND : QuadOp.LOGICAL_OR;
        this.add(new Quad(quadOp, left, right, Compiler.generateSymbol(DataType.getInt())));
    }
}
