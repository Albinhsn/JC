package se.liu.albhe576.project;

import java.util.LinkedList;
import java.util.Map;

public class InstructionList extends LinkedList<Instruction> {

    public static final Register[] LINUX_FLOAT_PARAM_LOCATIONS = new Register[]{Register.XMM0, Register.XMM1, Register.XMM2, Register.XMM3, Register.XMM4, Register.XMM5, Register.XMM6, Register.XMM7};
    public static final Register[] LINUX_GENERAL_PARAM_LOCATIONS = new Register[]{Register.RDI, Register.RSI, Register.RDX, Register.RCX, Register.R8, Register.R9};
    public void createSignExtend(Register dest, Register source){this.add(new Instruction(OperationType.MOVSX, new Address<>(dest), new Address<>(source)));}
    public void createConvert(OperationType op, Register dest, Register source){
        this.add(new Instruction(op, dest, source));
    }
    public void createCall(TemporaryVariableStack tempStack, String name, ImmediateSymbol stackSizeImmediate, Symbol returnSymbol) throws CompileException {
        this.add(new Instruction(OperationType.CALL, new Address<>(name)));
        int stackSize = Integer.parseInt(stackSizeImmediate.getValue());
        stackSize += Compiler.getStackAlignment(stackSize);
        if(stackSize != 0){
            this.deallocateStackSpace(stackSize);
        }
        if(!returnSymbol.type.isVoid()){
            this.addTemporary(tempStack, returnSymbol);
        }
    }
    public void createLoad(TemporaryVariableStack tempStack, VariableSymbol variable, QuadOp quadOp, Symbol result) throws CompileException {
        OperationType op = (variable.type.isStruct() || quadOp == QuadOp.LOAD_POINTER) ? OperationType.LEA : InstructionList.getMoveOpFromType(variable.type);
        this.loadStackVariable(op, Register.getPrimaryRegisterFromDataType(result.type), variable.offset);
        this.addTemporary(tempStack, result);
    }
    public void createMov(OperationType op, Register dest, Register source){this.add(new Instruction(op, dest, source));}
    public void createMov(OperationType op, Register dest, Address<?> source){this.add(new Instruction(op, dest, source));}
    public void createMov(OperationType op, Address<?> dest, Register source){this.add(new Instruction(op, dest, new Address<>(source)));}
    public void createRepeatMoveSingleByte(){this.add(new Instruction(OperationType.REP, new Address<>(Register.MOVSB)));}
    public void createJump(String name){this.add(new Instruction(OperationType.JMP, new Address<>(name)));}
    public void loadStackVariable(OperationType op, Register destination, int offset){
        this.add(new Instruction(op, destination, new Address<>(Register.RBP, true, offset)));
    }
    public void createDereference(TemporaryVariableStack tempStack, Symbol result) throws CompileException {
        Register primary = this.popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(getMoveOpFromType(result.type), Register.getPrimaryRegisterFromDataType(result.type), new Address<>(primary, true)));
        this.addTemporary(tempStack, result);
    }
    public void createIndex(SymbolTable symbolTable, TemporaryVariableStack tempStack, Symbol result, DataType pointerType, QuadOp op) throws CompileException {
        this.calculateIndex(symbolTable, tempStack, pointerType);
        if(op == QuadOp.INDEX){
            OperationType operationType = result.type.isStruct() ? OperationType.LEA : InstructionList.getMoveOpFromType(result.type);
            this.add(new Instruction(operationType, Register.getPrimaryRegisterFromDataType(result.type), new Address<>(Register.PRIMARY_GENERAL_REGISTER, true)));
        }
        this.addTemporary(tempStack, result);
    }
    public void createStoreArrayItem(SymbolTable symbolTable, TemporaryVariableStack tempStack, VariableSymbol arraySymbol, ImmediateSymbol indexSymbol){
        ArrayDataType arrayDataType = (ArrayDataType)arraySymbol.type;
        String index = indexSymbol.getValue();
        int offset = arraySymbol.offset + Integer.parseInt(index);
        if(arrayDataType.itemType.isStruct()){
            this.popIntoRegister(tempStack, Register.RSI);
            this.createLoadEffectiveAddress(Register.RDI, new Address<>(Register.RBP, true, offset));
            this.createMovSB(SymbolTable.getStructSize(symbolTable.getStructs(), arrayDataType.itemType));
            return;
        }
        this.popTemporaryIntoPrimary(tempStack);
        OperationType moveOp = InstructionList.getMoveOpFromType(arrayDataType.itemType);
        this.add(new Instruction(moveOp, new Address<>(Register.RBP, true, offset), Register.getPrimaryRegisterFromDataType(arrayDataType.itemType)));
    }
    public void createLoadMember(TemporaryVariableStack tempStack, ImmediateSymbol member, Symbol result, QuadOp op) throws CompileException {
        this.popTemporaryIntoPrimary(tempStack);
        OperationType operationType = (result.type.isStruct() || op == QuadOp.LOAD_MEMBER_POINTER) ? OperationType.LEA : InstructionList.getMoveOpFromType(result.type);
        this.add(new Instruction(operationType, Register.getPrimaryRegisterFromDataType(result.type), new Address<>(Register.RAX, true, Integer.parseInt(member.getValue()))));
        this.addTemporary(tempStack, result);
    }

    public void createParam(SymbolTable symbolTable, TemporaryVariableStack tempStack, ArgumentSymbol argumentSymbol, Symbol operand1){
        if(operand1.type.isStruct()){
            this.moveStruct(symbolTable, tempStack, argumentSymbol, operand1.type);
        }else {
            if(!argumentSymbol.getExternal()){
                this.popTemporaryIntoPrimary(tempStack);
                this.createMov(InstructionList.getMoveOpFromType(operand1.type), new Address<>(Register.RSP, true, argumentSymbol.getOffset()), Register.getPrimaryRegisterFromDataType(operand1.type));
            }else{
                this.addExternalParameter(tempStack, argumentSymbol, operand1.type);
            }
        }
    }

    public void moveStruct(SymbolTable symbolTable,TemporaryVariableStack tempStack, ArgumentSymbol argument, DataType structType){
        popIntoRegister(tempStack, Register.RSI);
        this.createLoadEffectiveAddress(Register.RDI, new Address<>(Register.RSP, true, argument.getOffset()));

        int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), structType);
        this.createMovSB(structSize);
    }
    public void assignStruct(SymbolTable symbolTable,TemporaryVariableStack tempStack, DataType structType){
        popIntoRegister(tempStack, Register.RDI);
        popIntoRegister(tempStack, Register.RSI);
        int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), structType);
        this.createMovSB(structSize);
    }
    public void createBinary(TemporaryVariableStack tempStack, OperationType op, Symbol result) throws CompileException {
        Register secondary   = this.popTemporaryIntoSecondary(tempStack);
        Register primary     = this.popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(op, primary, secondary));
        this.addTemporary(tempStack, result);
    }

    public void createMovSB(int immediate){
        this.createMov(OperationType.MOV, Register.RCX, new Address<>(immediate));
        this.createRepeatMoveSingleByte();
    }

    public static OperationType getMoveOpFromType(DataType type){
        if(!type.isFloatingPoint()){
            return OperationType.MOV;
        }else if(type.isFloat()) {
            return OperationType.MOVSS;
        }
        return OperationType.MOVSD;
    }
    public void popIntoRegister(TemporaryVariableStack tempStack, Register primary){
        VariableSymbol value                = tempStack.popVariable();
        OperationType moveOp                = getMoveOpFromType(value.type);
        this.createMov(moveOp, primary, new Address<>(Register.RBP, true, value.offset));
    }
    public Register popTemporaryIntoPrimary(TemporaryVariableStack tempStack){
        VariableSymbol value            = tempStack.peek();
        Register primary                 = Register.getPrimaryRegisterFromDataType(value.type);
        this.popIntoRegister(tempStack, primary);
        Operation lastOp = this.getLast().op;
        if(lastOp.getOp() != OperationType.MOVSX && value.type.isInteger() && !(value.type.isLong() || value.type.isInt())){
            this.createSignExtend(Register.RAX, primary);
        }
        return primary;
    }
    public void createShift(TemporaryVariableStack tempStack, OperationType op, Symbol result) throws CompileException {
        this.popTemporaryIntoSecondary(tempStack);
        Register primary     = this.popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(op, primary, new Address<>(Register.CL)));
        this.addTemporary(tempStack, result);
    }
    public void createNegate(TemporaryVariableStack tempStack, Symbol result) throws CompileException {
        Register primary = this.popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(OperationType.NOT, new Address<>(primary)));
        this.add(new Instruction(OperationType.INC, new Address<>(primary)));
        this.addTemporary(tempStack, result);
    }
    public void createMod(TemporaryVariableStack tempStack, Symbol result, DataType targetType) throws CompileException {
        this.add(new Instruction(OperationType.XOR, Register.RDX, Register.RDX));
        Register secondary = Register.getSecondaryRegisterFromDataType(targetType);
        this.popIntoRegister(tempStack, secondary);
        this.popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(OperationType.IDIV, new Address<>(secondary)));
        this.add(new Instruction(InstructionList.getMoveOpFromType(result.type), Register.getPrimaryRegisterFromDataType(result.type), Register.getThirdRegisterFromDataType(result.type)));
        this.addTemporary(tempStack, result);
    }
    public void addLabel(String name){this.add(new Instruction(name));}
    public void createReturn(TemporaryVariableStack tempStack, Symbol result){
        if(result != null){
            this.popTemporaryIntoPrimary(tempStack);
        }
        this.addEpilogue();
        this.addReturn();
    }
    public void createLoadImmediate(Map<String,Constant> constants,TemporaryVariableStack tempStack, ImmediateSymbol immediateSymbol, Symbol result) throws CompileException {
        if(result.type.isString() || result.type.isFloatingPoint()) {
            Constant constant = constants.get(immediateSymbol.getValue());
            this.add(new Instruction(InstructionList.getMoveOpFromType(result.type), Register.getPrimaryRegisterFromDataType(result.type), new Address<>(constant.label(), !result.type.isString())));
        }else{
            this.add(new Instruction(InstructionList.getMoveOpFromType(result.type), Register.getPrimaryRegisterFromDataType(result.type), new Address<>(immediateSymbol.getValue())));
        }
        this.addTemporary(tempStack, result);
    }
    public void createIMul(TemporaryVariableStack tempStack, ImmediateSymbol immediateSymbol, Symbol result) throws CompileException {
        Register primary = this.popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(OperationType.IMUL, primary, new Address<>(immediateSymbol.getValue())));
        this.addTemporary(tempStack, result);
    }
    public void createAssignImmediate(VariableSymbol variable, ImmediateSymbol immediate){
        OperationSize size = OperationSize.getSizeFromType(variable.type);
        Operation operation = new Operation(InstructionList.getMoveOpFromType(variable.type), size);
        this.add(new Instruction(operation, new Address<>(Register.RBP, true, variable.offset), new Address<>(immediate.getValue())));

    }
    public void createAssign(SymbolTable symbolTable, TemporaryVariableStack tempStack, DataType targetType){
        if(targetType.isStruct()){
            this.assignStruct(symbolTable, tempStack, targetType);
        }else{
            VariableSymbol pointer  = tempStack.popVariable();
            VariableSymbol value    = tempStack.popVariable();
            this.add(new Instruction(InstructionList.getMoveOpFromType(value.type), Register.getPrimaryRegisterFromDataType(value.type), new Address<>(Register.RBP, true, value.offset)));
            this.add(new Instruction(InstructionList.getMoveOpFromType(pointer.type), Register.getSecondaryRegisterFromDataType(pointer.type), new Address<>(Register.RBP, true, pointer.offset)));
            this.add(new Instruction(InstructionList.getMoveOpFromType(targetType), new Address<>(Register.RCX, true), Register.getPrimaryRegisterFromDataType(targetType)));
        }

    }
    public void createNotFloat(TemporaryVariableStack tempStack, Symbol result, DataType targetType) throws CompileException {
        Register primary = this.popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(OperationType.MOV, Register.RAX, new Address<>(0)));
        OperationType op = Operation.getCmpOpFromType(targetType);
        OperationType moveOp = Operation.getConvertOpFromType(DataType.getInt(), targetType);
        this.add(new Instruction(moveOp, Register.SECONDARY_SSE_REGISTER, Register.RAX));
        this.createCompare(op, OperationType.SETE, primary, new Address<>(Register.SECONDARY_SSE_REGISTER));
        this.addTemporary(tempStack, result);
    }
    public void createNotInteger(TemporaryVariableStack tempStack, Symbol result) throws CompileException {
        Register primary = this.popTemporaryIntoPrimary(tempStack);
        this.createCompare(OperationType.CMP, OperationType.SETE, primary, new Address<>(0));
        this.addTemporary(tempStack, result);
    }
    public void addExternalParameter(TemporaryVariableStack tempStack, ArgumentSymbol argumentSymbol, DataType paramType) {
        VariableSymbol param = tempStack.peek();
        popTemporaryIntoPrimary(tempStack);
        Register[] registers = paramType.isFloatingPoint() ? LINUX_FLOAT_PARAM_LOCATIONS : LINUX_GENERAL_PARAM_LOCATIONS;
        if(argumentSymbol.getCount() >= registers.length){
            this.add(new Instruction(getMoveOpFromType(paramType), new Address<>(Register.RSP, true, argumentSymbol.getOffset()), Register.getPrimaryRegisterFromDataType(paramType)));
        }else{
            Register source = param.type.isFloatingPoint() ? Register.PRIMARY_SSE_REGISTER : Register.PRIMARY_GENERAL_REGISTER;
            this.add(new Instruction(getMoveOpFromType(param.type), registers[argumentSymbol.getCount()], source));
        }
    }
    public void createLogical(SymbolTable symbolTable, TemporaryVariableStack tempStack, QuadOp op, Symbol result) throws CompileException {
        Register right = popTemporaryIntoSecondary(tempStack);
        Register left = popTemporaryIntoPrimary(tempStack);
        int firstRegister = op == QuadOp.LOGICAL_OR ? 1 : 0;

        this.add(new Instruction(OperationType.CMP, left, new Address<>(firstRegister)));
        String mergeLabel        = symbolTable.generateLabel().name;

        this.add(new Instruction(OperationType.JE, new Address<>(mergeLabel)));
        createCompare(OperationType.CMP, OperationType.SETE, right, new Address<>(1));
        this.add(new Instruction(mergeLabel));
        addTemporary(tempStack, result);
    }
    public void calculateIndex(SymbolTable symbolTable, TemporaryVariableStack tempStack, DataType pointerType) throws CompileException {
        popTemporaryIntoPrimary(tempStack);
        popTemporaryIntoSecondary(tempStack);

        int size = symbolTable.getStructSize(pointerType.getTypeFromPointer());
        this.add(new Instruction(OperationType.IMUL, Register.PRIMARY_GENERAL_REGISTER, new Address<>(size)));
        this.add(new Instruction(OperationType.ADD, Register.PRIMARY_GENERAL_REGISTER, Register.SECONDARY_GENERAL_REGISTER));
    }

    public void createJumpOnCondition(TemporaryVariableStack tempStack, boolean jumpOnTrue, Symbol dest){
        Address<?> immediate = new Address<>(jumpOnTrue ? 1 : 0);
        popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(Operation.getCmpOpFromType(dest.type), Register.PRIMARY_GENERAL_REGISTER, immediate));
        this.add(new Instruction(OperationType.JE, new Address<>(dest.name)));
    }

    public void addTemporary(TemporaryVariableStack tempStack, Symbol result) throws CompileException {
        int offset;
        if(result.type.isStruct()){
            offset = tempStack.pushVariable(result.name, result.type.getPointerFromType());
        }else{
            offset = tempStack.pushVariable(result.name, result.type);
        }
        OperationType moveOp = getMoveOpFromType(result.type);
        this.createMov(moveOp, new Address<>(Register.RBP, true, offset), Register.getPrimaryRegisterFromDataType(result.type));
    }
    public void createComparison(TemporaryVariableStack tempStack, OperationType op, DataType destType, Symbol result) throws CompileException {
        Register right = popTemporaryIntoSecondary(tempStack);
        Register left = popTemporaryIntoPrimary(tempStack);
        createCompare(Operation.getCmpOpFromType(destType), op, left, new Address<>(right));
        this.addTemporary(tempStack, result);
    }
    public void deallocateStackSpace(int space){this.add(new Instruction(OperationType.ADD, Register.RSP, new Address<>(space)));}
    public void allocateStackSpace(int space){this.add(new Instruction(OperationType.SUB, Register.RSP, new Address<>(space)));}
    public void allocateStackSpace(int space, int index){this.add(index, new Instruction(OperationType.SUB, Register.RSP, new Address<>(space)));}

    public void createCompare(OperationType compareOp, OperationType setOp, Register left, Address<?> right){
        this.add(new Instruction(compareOp, left, right));
        this.add(new Instruction(setOp, new Address<>(Register.AL)));
    }
    public Register popTemporaryIntoSecondary(TemporaryVariableStack tempStack){
        VariableSymbol value                = tempStack.peek();
        Register secondary                  = Register.getSecondaryRegisterFromDataType(value.type);
        popIntoRegister(tempStack, secondary);
        if(value.type.isInteger() && !value.type.isLong()){
            this.createSignExtend(Register.RCX, secondary);
        }
        return secondary;
    }
    public void createLoadEffectiveAddress(Register target, Address<?> source){
        this.add(new Instruction(OperationType.LEA, target, source));
    }
    public void addPrologue(){
        this.add(new Instruction(OperationType.PUSH, new Address<>(Register.RBP)));
        this.createMov(OperationType.MOV, Register.RBP, Register.RSP);
    }
    public void addEpilogue(){
        this.createMov(OperationType.MOV, Register.RSP, Register.RBP);
        this.add(new Instruction(OperationType.POP, new Address<>(Register.RBP)));
    }
    public void addReturn(){this.add(new Instruction(OperationType.RET));}

    public void convertAddress(TemporaryVariableStack tempStack, Symbol result, Symbol dest) throws CompileException {
        if(dest.type.isInteger() && result.type.isInteger() && dest.type.isSameType(DataType.getHighestDataTypePrecedence(dest.type, result.type))){
            return;
        }
        popTemporaryIntoPrimary(tempStack);

        OperationType convert = Operation.getConvertOpFromType(dest.type, result.type);
        Register target = Register.getMinimumConvertTarget(convert, result.type);
        Register source = Register.getMinimumConvertSource(convert, dest.type);
        this.createConvert(convert, target, source);
        addTemporary(tempStack, result);
    }

    public void createPrefixDecFloat(TemporaryVariableStack tempStack, Symbol result) throws CompileException {
        this.setupPostfixFloat(tempStack, result, result.type);
        this.immediateArithmeticFloat(result.type.isDouble() ? OperationType.SUBSD : OperationType.SUBSS, result);
        this.addTemporary( tempStack, result);
    }
    public void createPrefixIncFloat(TemporaryVariableStack tempStack, Symbol result) throws CompileException {
        this.setupPostfixFloat(tempStack, result, result.type);
        this.immediateArithmeticFloat(result.type.isDouble() ? OperationType.ADDSD : OperationType.ADDSS, result);
        this.addTemporary(tempStack, result);
    }
    public void createPostfixDecFloat(TemporaryVariableStack tempStack, Symbol result) throws CompileException {
        this.setupPostfixFloat(tempStack, result, result.type);
        this.addTemporary(tempStack, result);
        this.immediateArithmeticFloat(result.type.isDouble() ? OperationType.SUBSD : OperationType.SUBSS, result);
    }
    public void createIntegerDiv(TemporaryVariableStack tempStack, Symbol result) throws CompileException {
        Register secondary = this.popTemporaryIntoSecondary(tempStack);
        this.popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(OperationType.XOR, Register.RDX, Register.RDX));
        this.add(new Instruction(OperationType.IDIV, new Address<>(secondary)));
        this.addTemporary(tempStack, result);
    }

    public void createIntegerMul(TemporaryVariableStack tempStack, Symbol result) throws CompileException {
        this.popTemporaryIntoPrimary(tempStack);
        Register secondary = this.popTemporaryIntoSecondary(tempStack);
        this.add(new Instruction(OperationType.MUL, new Address<>(secondary)));
        this.addTemporary(tempStack, result);
    }
    public void createPostfixIncFloat(TemporaryVariableStack tempStack, Symbol result, DataType target) throws CompileException {
        this.setupPostfixFloat(tempStack, result, target);
        this.addTemporary(tempStack, result);
        this.immediateArithmeticFloat(result.type.isDouble() ? OperationType.ADDSD : OperationType.ADDSS, result);
    }

    public void setupPostfixFloat(TemporaryVariableStack tempStack, Symbol result, DataType target) {
        this.popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(InstructionList.getMoveOpFromType(result.type), Register.getPrimaryRegisterFromDataType(target), new Address<>(Register.RAX, true)));

        this.add(new Instruction(OperationType.MOV, Register.SECONDARY_GENERAL_REGISTER, new Address<>(1)));
        OperationType convertOp = result.type.isDouble() ? OperationType.CVTSI2SD : OperationType.CVTSI2SS;
        this.add(new Instruction(convertOp, Register.SECONDARY_SSE_REGISTER, Register.SECONDARY_GENERAL_REGISTER));
    }
    public void immediateArithmeticFloat(OperationType arithmeticOp, Symbol result){
        OperationType moveOp = InstructionList.getMoveOpFromType(result.type);
        this.add(new Instruction(arithmeticOp, Register.PRIMARY_SSE_REGISTER, Register.SECONDARY_SSE_REGISTER));
        this.add(new Instruction(moveOp, new Address<>(Register.RAX, true), Register.PRIMARY_SSE_REGISTER));
    }
    public void createPostfixInteger(TemporaryVariableStack tempStack, OperationType op, OperationType pointerOp, boolean post, Symbol result, DataType target) throws CompileException {
        this.popTemporaryIntoSecondary( tempStack);
        Address<?> primary = new Address<>(Register.getPrimaryRegisterFromDataType(target));
        this.add(new Instruction(OperationType.MOV, primary, new Address<>(Register.RCX, true)));
        if(post){
            this.addTemporary(tempStack, result);
        }
        if(target.isPointer()){
            int pointerSize = target.getTypeFromPointer().getSize();
            this.add(new Instruction(pointerOp, primary, new Address<>(pointerSize)));
        }else{
            this.add(new Instruction(op, primary));
        }
        this.add(new Instruction(OperationType.MOV, new Address<>(Register.RCX, true), primary));

        if(!post){
            this.addTemporary(tempStack, result);
        }
    }
}
