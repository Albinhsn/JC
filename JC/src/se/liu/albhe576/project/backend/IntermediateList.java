package se.liu.albhe576.project.backend;

import se.liu.albhe576.project.frontend.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A container class for intermediates. Has helper functions for creation and querying the contained intermediates
 * @see Intermediate
 * @see IntermediateOperation
 * @see Symbol
 */
public class IntermediateList implements Iterable<Intermediate>{
    private final List<Intermediate> intermediates;
    public Iterator<Intermediate> iterator(){
        return intermediates.iterator();
    }

    public int size(){
        return intermediates.size();
    }
    public void addAll(IntermediateList intermediates){
        this.intermediates.addAll(intermediates.intermediates);
    }

    public Intermediate pop(){
        Intermediate out = this.intermediates.get(this.intermediates.size() - 1);
        this.intermediates.remove(this.intermediates.size() - 1);
        return out;
    }
    public Intermediate getLastIntermediate(){return this.intermediates.get(this.intermediates.size() - 1);}
    public Symbol getLastOperand1(){return this.getLastIntermediate().operand1();}
    public Symbol getLastResult(){return this.getLastIntermediate().result();}
    public void insertLabel(Symbol label){this.intermediates.add(new Intermediate(IntermediateOperation.LABEL, label, null, null));}
    public void createLogical(SymbolTable symbolTable, Symbol left, Symbol right, TokenType op) throws CompileException {
        this.intermediates.add(new Intermediate(IntermediateOperation.fromToken(op), left, right, symbolTable.generateSymbol(DataType.getByte())));
    }
    public void createStoreArray(ArrayItemSymbol arraySymbol){
        this.intermediates.add(new Intermediate(IntermediateOperation.STORE_ARRAY_ITEM, arraySymbol, null, null));
    }
    public void createReturn(Symbol toBeReturned){
        this.intermediates.add(new Intermediate(IntermediateOperation.RET, null, null, toBeReturned));
    }
    public void createCall(SymbolTable symbolTable, FunctionSymbol function){
        this.intermediates.add(new Intermediate(IntermediateOperation.CALL, function, null, symbolTable.generateSymbol(function.type)));
    }
    public void createDereference(SymbolTable symbolTable, Symbol source) {
        this.intermediates.add(new Intermediate(IntermediateOperation.DEREFERENCE, source, null, symbolTable.generateSymbol(source.type.getTypeFromPointer())));
    }
    public void createNegate(SymbolTable symbolTable, Symbol source){
        this.intermediates.add(new Intermediate(IntermediateOperation.NEGATE, source, null, symbolTable.generateSymbol(source.type)));
    }
    public void createLogicalNot(SymbolTable symbolTable, Symbol source, IntermediateOperation op){
        this.intermediates.add(new Intermediate(op, source, null, symbolTable.generateSymbol(DataType.getByte())));
    }
    public Symbol createConvert(SymbolTable symbolTable, Symbol source, DataType target){
        Symbol converted = symbolTable.generateSymbol(target);
        this.intermediates.add(new Intermediate(IntermediateOperation.CONVERT, source, null, converted));
        return converted;
    }
    public void createJumpCondition(Symbol label, boolean jumpTrue){
        IntermediateOperation op = jumpTrue ? IntermediateOperation.JMP_T : IntermediateOperation.JMP_F;
        this.intermediates.add(new Intermediate(op, label, null, null));
    }
    public void createPostfix(Symbol target, TokenType op){
        IntermediateOperation intermediateOperation = op == TokenType.TOKEN_INCREMENT ? IntermediateOperation.POST_INC : IntermediateOperation.POST_DEC;
        this.intermediates.add(new Intermediate(intermediateOperation, target, null, target));

    }
    public void createPrefix(Symbol target, TokenType op){
        IntermediateOperation intermediateOperation = op == TokenType.TOKEN_INCREMENT ? IntermediateOperation.PRE_INC : IntermediateOperation.PRE_DEC;
        this.intermediates.add(new Intermediate(intermediateOperation, target, null, target));
    }
    public void createJump(Symbol label){
        this.intermediates.add(new Intermediate(IntermediateOperation.JMP, label, null, null));
    }
    public void createLoadImmediate(SymbolTable symbolTable, ImmediateSymbol immediate){
        this.intermediates.add(new Intermediate(IntermediateOperation.LOAD_IMM, immediate, null, symbolTable.generateSymbol(immediate.type)));
    }
    public void createMember(SymbolTable symbolTable, MemberSymbol source, DataType memberType){
        this.intermediates.add(new Intermediate(IntermediateOperation.LOAD_MEMBER, source, null, symbolTable.generateSymbol(memberType)));
    }
    public void createIndex(SymbolTable symbolTable, Symbol value, Symbol index) {
        this.intermediates.add(new Intermediate(IntermediateOperation.INDEX, value, index, symbolTable.generateSymbol(value.type.getTypeFromPointer())));
    }

    public void createBinaryOp(SymbolTable symbolTable, IntermediateOperation op, Symbol left, Symbol right, DataType result) {
        this.intermediates.add(new Intermediate(op, left, right, symbolTable.generateSymbol(result)));
    }
    public void createCast(SymbolTable symbolTable, Symbol value, DataType target){
        this.intermediates.add(new Intermediate(IntermediateOperation.CAST, value, null, symbolTable.generateSymbol(target)));
    }

    public void createComparison(SymbolTable symbolTable, IntermediateOperation op, Symbol left, Symbol right) {
        this.intermediates.add(new Intermediate(op, left, right, symbolTable.generateSymbol(DataType.getByte())));
    }
    public void createImmediateMultiply(SymbolTable symbolTable, Symbol target, int immediate){
        String immediateString = String.valueOf(immediate);
        this.intermediates.add(new Intermediate(IntermediateOperation.IMUL, target, new ImmediateSymbol(immediateString, DataType.getLong(), immediateString), symbolTable.generateSymbol(DataType.getLong())));
    }
    public void createLoadPointer(SymbolTable symbolTable, Symbol pointer) {
       this.intermediates.add(new Intermediate(IntermediateOperation.LOAD_POINTER, pointer, null, symbolTable.generateSymbol(pointer.type.getPointerFromType())));
    }
    public void createLoadMemberPointer(SymbolTable symbolTable, MemberSymbol pointer, DataType memberType) {
        this.intermediates.add(new Intermediate(IntermediateOperation.LOAD_MEMBER_POINTER, pointer, null, symbolTable.generateSymbol(memberType.getPointerFromType())));
    }
    public void createReferenceIndex(SymbolTable symbolTable, Symbol value, Symbol index) {
        this.intermediates.add(new Intermediate(IntermediateOperation.REFERENCE_INDEX, value, index, symbolTable.generateSymbol(value.type)));
    }

    public void createLoad(SymbolTable symbolTable, VariableSymbol symbol) {
        if(symbol.type.isArray()){
            createLoadPointer(symbolTable, symbol);
        }else{
            this.intermediates.add(new Intermediate(IntermediateOperation.LOAD, symbol, null, symbolTable.generateSymbol(symbol.type)));
        }
    }
    public void createAssign(Symbol value, Symbol variable){
        this.intermediates.add(new Intermediate(IntermediateOperation.ASSIGN, value, null, variable));
    }
    public IntermediateList(){
        this.intermediates = new ArrayList<>();
    }
}
