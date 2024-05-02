package se.liu.albhe576.project;

import java.util.LinkedList;

public class InstructionList extends LinkedList<Instruction> {

    public static final Register[] LINUX_FLOAT_PARAM_LOCATIONS = new Register[]{Register.XMM0, Register.XMM1, Register.XMM2, Register.XMM3, Register.XMM4, Register.XMM5, Register.XMM6, Register.XMM7};
    public static final Register[] LINUX_GENERAL_PARAM_LOCATIONS = new Register[]{Register.RDI, Register.RSI, Register.RDX, Register.RCX, Register.R8, Register.R9};
    public void createSignExtend(Register dest, Register source){
       this.add(new Instruction(OperationType.MOVSX, dest, source));
    }
    public void createConvert(OperationType op, Register dest, Register source){
        this.add(new Instruction(op, dest, source));
    }
    public void createMov(OperationType op, Register dest, Immediate source){this.add(new Instruction(op, dest, source));}
    public void createMov(OperationType op, Register dest, Register source){this.add(new Instruction(op, dest, source));}
    public void createMov(OperationType op, Register dest, Address source){this.add(new Instruction(op, dest, source));}
    public void createMov(OperationType op, Address dest, Register source){this.add(new Instruction(op, dest, new Address(source)));}
    public void createRepeatMoveSingleByte(){this.add(new Instruction(OperationType.REP, new Operation(OperationType.MOVSB)));}

    public void moveStruct(SymbolTable symbolTable,TemporaryVariableStack tempStack, ArgumentSymbol argument, DataType structType){
        popIntoRegister(tempStack, Register.RSI);
        this.createLoadEffectiveAddress(Register.RDI, new Address(Register.RSP, true, argument.getOffset()));

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
        this.createMov(OperationType.MOV, Register.RCX, new Immediate(immediate));
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
        VariableSymbol value            = tempStack.popVariable();
        OperationType moveOp                = getMoveOpFromType(value.type);
        this.createMov(moveOp, primary, new Address(Register.RBP, true, value.offset));
    }
    public Register popTemporaryIntoPrimary(TemporaryVariableStack tempStack){
        VariableSymbol value            = tempStack.peek();
        Register primary                 = Register.getPrimaryRegisterFromDataType(value.type);
        this.popIntoRegister(tempStack, primary);
        if(value.type.isInteger() && !(value.type.isLong() || value.type.isInt())){
            this.createSignExtend(Register.RAX, primary);
        }
        return primary;
    }
    public void createShift(TemporaryVariableStack tempStack, OperationType op, Symbol result) throws CompileException {
        this.popTemporaryIntoSecondary(tempStack);
        Register primary     = this.popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(op, primary, new Address(Register.CL)));
        this.addTemporary(tempStack, result);
    }
    public void addExternalParameter(TemporaryVariableStack tempStack, ArgumentSymbol argumentSymbol, DataType paramType) {
        VariableSymbol param = tempStack.peek();
        popTemporaryIntoPrimary(tempStack);
        Register[] registers = paramType.isFloatingPoint() ? LINUX_FLOAT_PARAM_LOCATIONS : LINUX_GENERAL_PARAM_LOCATIONS;
        if(argumentSymbol.getCount() >= registers.length){
            this.add(new Instruction(getMoveOpFromType(paramType), new Address(Register.RSP, true, argumentSymbol.getOffset()), Register.getPrimaryRegisterFromDataType(paramType)));
        }else{
            Register source = param.type.isFloatingPoint() ? Register.PRIMARY_SSE_REGISTER : Register.PRIMARY_GENERAL_REGISTER;
            this.add(new Instruction(getMoveOpFromType(param.type), registers[argumentSymbol.getCount()], source));
        }
    }
    public void createLogical(SymbolTable symbolTable, TemporaryVariableStack tempStack, QuadOp op, Symbol result) throws CompileException {
        Register right = popTemporaryIntoSecondary(tempStack);
        Register left = popTemporaryIntoPrimary(tempStack);
        int firstRegister = op == QuadOp.LOGICAL_OR ? 1 : 0;

        this.add(new Instruction(OperationType.CMP, left, new Immediate(firstRegister)));
        String mergeLabel        = symbolTable.generateLabel().name;

        this.add(new Instruction(OperationType.JE, new Immediate(mergeLabel)));
        createCompare(OperationType.CMP, OperationType.SETE, right, new Immediate(1));
        this.add(new Instruction(new Label(mergeLabel)));
        addTemporary(tempStack, result);
    }
    public void calculateIndex(SymbolTable symbolTable, TemporaryVariableStack tempStack, DataType pointerType) throws CompileException {
        popTemporaryIntoPrimary(tempStack);
        popTemporaryIntoSecondary(tempStack);

        int size = symbolTable.getStructSize(pointerType.getTypeFromPointer());
        this.add(new Instruction(OperationType.IMUL, Register.PRIMARY_GENERAL_REGISTER, new Immediate(size)));
        this.add(new Instruction(OperationType.ADD, Register.PRIMARY_GENERAL_REGISTER, Register.SECONDARY_GENERAL_REGISTER));
    }

    public void createJumpOnCondition(TemporaryVariableStack tempStack, boolean jumpOnTrue, Symbol dest){
        Immediate immediate = new Immediate(jumpOnTrue ? 1 : 0);
        popTemporaryIntoPrimary(tempStack);
        this.add(new Instruction(Operation.getCmpOpFromType(dest.type), Register.PRIMARY_GENERAL_REGISTER, immediate));
        this.add(new Instruction(OperationType.JE, new Immediate(dest.name)));
    }

    public void addTemporary(TemporaryVariableStack tempStack, Symbol result) throws CompileException {
        int offset;
        if(result.type.isStruct()){
            offset = tempStack.pushVariable(result.name, result.type.getPointerFromType());
        }else{
            offset = tempStack.pushVariable(result.name, result.type);
        }
        OperationType moveOp = getMoveOpFromType(result.type);
        this.createMov(moveOp, new Address(Register.RBP, true, offset), Register.getPrimaryRegisterFromDataType(result.type));
    }
    public void createComparison(TemporaryVariableStack tempStack, OperationType op, DataType destType, Symbol result) throws CompileException {
        Register right = popTemporaryIntoSecondary(tempStack);
        Register left = popTemporaryIntoPrimary(tempStack);
        createCompare(Operation.getCmpOpFromType(destType), op, left, new Address(right));
        this.addTemporary(tempStack, result);
    }
    public void allocateStackSpace(int space){this.add(new Instruction(OperationType.SUB, Register.RSP, new Immediate(space)));}
    public void allocateStackSpace(int space, int index){this.add(index, new Instruction(OperationType.SUB, Register.RSP, new Immediate(space)));}

    public void createCompare(OperationType compareOp, OperationType setOp, Register left, Operand right){
        this.add(new Instruction(compareOp, left, right));
        this.add(new Instruction(setOp, new Address(Register.AL)));
        this.createSignExtend(Register.RAX, Register.AL);
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
    public void createLoadEffectiveAddress(Register target, Address source){
        this.add(new Instruction(OperationType.LEA, target, source));
    }
    public void addPrologue(){
        this.add(new Instruction(OperationType.PUSH, new Address(Register.RBP)));
        this.createMov(OperationType.MOV, Register.RBP, Register.RSP);
    }
    public void addEpilogue(){
        this.createMov(OperationType.MOV, Register.RSP, Register.RBP);
        this.add(new Instruction(OperationType.POP, new Address(Register.RBP)));
    }
    public void addReturn(){this.add(new Instruction(new Operation(OperationType.RET)));}

    public void convertAddress(TemporaryVariableStack tempStack, Symbol result, Symbol dest) throws CompileException {
        if(dest.type.isInteger() && result.type.isInteger() && dest.type.isSameType(DataType.getHighestDataTypePrecedence(dest.type, result.type))){
            return;
        }
        Register primary = popTemporaryIntoPrimary(tempStack);
        if(!(dest.type.isLong() || dest.type.isInt()) && dest.type.isInteger()){
            this.createSignExtend(Register.RAX, primary);
        }

        OperationType convert = Operation.getConvertOpFromType(dest.type, result.type);
        Register target = Register.getMinimumConvertTarget(convert, result.type);
        Register source = Register.getMinimumConvertSource(convert, dest.type);
        this.createConvert(convert, target, source);
        addTemporary(tempStack, result);
    }
}
