package se.liu.albhe576.project;

import java.util.ArrayList;

public class QuadList extends ArrayList<Quad>{

    public Quad pop(){
        Quad out = this.get(this.size() - 1);
        this.remove(this.size() - 1);
        return out;
    }
    public Quad getLastQuad(){return this.get(this.size() - 1);}
    public Symbol getLastOperand1(){return this.getLastQuad().operand1();}
    public Symbol getLastResult(){return this.getLastQuad().result();}
    public void insertLabel(Symbol label){this.add(new Quad(QuadOp.LABEL, label, null, null));}
    public void createLogical(SymbolTable symbolTable, Symbol left, Symbol right, TokenType op) throws CompileException {
        this.add(new Quad(QuadOp.fromToken(op), left, right, symbolTable.generateSymbol(DataType.getByte())));
    }
    public void createStoreArray(VariableSymbol arraySymbol, int offset){
        this.add(new Quad(QuadOp.STORE_ARRAY_ITEM, new ArrayItemSymbol(arraySymbol.name, arraySymbol.type, arraySymbol.offset, offset),null, null));
    }
    public void createReturn(Symbol toBeReturned){
        this.add(new Quad(QuadOp.RET, null, null, toBeReturned));
    }
    public void createCall(SymbolTable symbolTable, Symbol function, int argumentSize){
        String argumentSizeString = String.valueOf(argumentSize);
        this.add(new Quad(QuadOp.CALL, function, new ImmediateSymbol(argumentSizeString, DataType.getInt(), argumentSizeString), symbolTable.generateSymbol(function.type)));
    }
    public void createDereference(SymbolTable symbolTable, Symbol source) throws CompileException {
        this.add(new Quad(QuadOp.DEREFERENCE, source, null, symbolTable.generateSymbol(source.type.getTypeFromPointer())));
    }
    public void createNegate(SymbolTable symbolTable, Symbol source){
        this.add(new Quad(QuadOp.NEGATE, source, null, symbolTable.generateSymbol(source.type)));
    }
    public void createLogicalNot(SymbolTable symbolTable, Symbol source, QuadOp op){
        this.add(new Quad(op, source, null, symbolTable.generateSymbol(DataType.getByte())));
    }
    public Symbol createConvert(SymbolTable symbolTable, Symbol source, DataType target){
        Symbol converted = symbolTable.generateSymbol(target);
        this.add(new Quad(QuadOp.CONVERT, source, null, converted));
        return converted;
    }
    public void createJumpCondition(Symbol label, boolean jumpTrue){
        QuadOp op = jumpTrue ? QuadOp.JMP_T : QuadOp.JMP_F;
        this.add(new Quad(op, label, null, null));
    }
    public void createPostfix(Symbol target, TokenType op){
        QuadOp quadOp = op == TokenType.TOKEN_INCREMENT ? QuadOp.POST_INC : QuadOp.POST_DEC;
        this.add(new Quad(quadOp, target, null, target));

    }
    public void createPrefix(Symbol target, TokenType op){
        QuadOp quadOp = op == TokenType.TOKEN_INCREMENT ? QuadOp.PRE_INC : QuadOp.PRE_DEC;
        this.add(new Quad(quadOp, target, null, target));
    }
    public void createJump(Symbol label){
        this.add(new Quad(QuadOp.JMP, label, null, null));
    }
    public void createLoadImmediate(SymbolTable symbolTable, ImmediateSymbol immediate){
        this.add(new Quad(QuadOp.LOAD_IMM, immediate, null, symbolTable.generateSymbol(immediate.type)));
    }
    public void createMember(SymbolTable symbolTable, MemberSymbol source, DataType memberType){
        this.add(new Quad(QuadOp.LOAD_MEMBER, source, null, symbolTable.generateSymbol(memberType)));
    }
    public void createIndex(SymbolTable symbolTable, Symbol value, Symbol index) throws CompileException {
        this.add(new Quad(QuadOp.INDEX, value, index, symbolTable.generateSymbol(value.type.getTypeFromPointer())));
    }

    public void createBinaryOp(SymbolTable symbolTable, QuadOp op, Symbol left, Symbol right, DataType result) {
        this.add(new Quad(op, left, right, symbolTable.generateSymbol(result)));
    }
    public void createParam(Symbol param, int offset, int count, boolean external){
        ArgumentSymbol argumentSymbol = new ArgumentSymbol(param.name, param.type, offset, count, external);
        this.add(new Quad(QuadOp.ARGUMENT, argumentSymbol, null, null));
    }
    public void createCast(SymbolTable symbolTable, Symbol value, DataType target){
        this.add(new Quad(QuadOp.CAST, value, null, symbolTable.generateSymbol(target)));
    }

    public void createComparison(SymbolTable symbolTable, QuadOp op, Symbol left, Symbol right) {
        this.add(new Quad(op, left, right, symbolTable.generateSymbol(DataType.getByte())));
    }
    public void createIMul(SymbolTable symbolTable, Symbol target, int immediate){
        String imm = String.valueOf(immediate);
        this.add(new Quad(QuadOp.IMUL, target, new ImmediateSymbol(imm, DataType.getLong(), imm), symbolTable.generateSymbol(DataType.getLong())));
    }
    public void createAllocate(int size){
        this.add(new Quad(QuadOp.ALLOCATE, new Symbol(String.valueOf(size), DataType.getInt()), null, null));
    }

    public void createLoadPointer(SymbolTable symbolTable, Symbol pointer) {
       this.add(new Quad(QuadOp.LOAD_POINTER, pointer, null, symbolTable.generateSymbol(pointer.type.getPointerFromType())));
    }
    public void createLoadMemberPointer(SymbolTable symbolTable, MemberSymbol pointer, DataType memberType) {
        this.add(new Quad(QuadOp.LOAD_MEMBER_POINTER, pointer, null, symbolTable.generateSymbol(memberType.getPointerFromType())));
    }
    public void createReferenceIndex(SymbolTable symbolTable, Symbol value, Symbol index) {
        this.add(new Quad(QuadOp.REFERENCE_INDEX, value, index, symbolTable.generateSymbol(value.type)));
    }

    public void createLoad(SymbolTable symbolTable, VariableSymbol symbol) {
        if(symbol.type.isArray()){
            createLoadPointer(symbolTable, symbol);
        }else{
            this.add(new Quad(QuadOp.LOAD, symbol, null, symbolTable.generateSymbol(symbol.type)));
        }
    }
    public void createAssign(Symbol value, Symbol variable){
        this.add(new Quad(QuadOp.ASSIGN, value, null, variable));
    }
}
