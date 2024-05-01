package se.liu.albhe576.project;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result) {
    @Override
    public String toString() {
        return String.format("%s %s %s %s",op.name(),operand1,operand2,result);
    }

    private Register getMinimumConvertSource(Operation op, DataType source) throws CompileException {
        switch(op){
            case MOVSX, CVTSS2SI, CVTTSD2SI -> {return Register.getPrimaryRegisterFromDataType(source);}
            case CVTSS2SD, CVTSD2SS -> {
                return Register.XMM0;
            }
            case CVTSI2SS -> {
                if(source.isLong()){
                    return Register.RAX;
                }
                return Register.EAX;
            }
            case CVTSI2SD -> {
                return Register.RAX;
            }
        }
        throw new CompileException(String.format("Can't get convert target from %s", op.name()));
    }
    private Register getMinimumConvertTarget(Operation op, DataType target) throws CompileException {
        switch(op){
            case MOVSX -> {return Register.RAX;}
            case CVTSD2SS, CVTSI2SD, CVTSS2SD, CVTSI2SS -> {return Register.XMM0;}
            case CVTSS2SI, CVTTSD2SI -> {
                if(target.isLong()){
                    return Register.RAX;
                }
                return Register.EAX;
            }
        }
        throw new CompileException(String.format("Can't get convert target from %s", op.name()));
    }

    public void convertOperand(List<Instruction> instructions, TemporaryVariableStack tempStack) throws CompileException {
        if(operand1.type.isInteger() && result.type.isInteger() && operand1.type.isSameType(DataType.getHighestDataTypePrecedence(operand1.type, result.type))){
            return;
        }
        Address primary = popTemporaryIntoPrimary(instructions, tempStack);
        if(!(operand1.type.isLong() || operand1.type.isInt()) && operand1.type.isInteger()){
            instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), primary));
        }

        Operation convert = getConvertOpFromType(operand1.type, result.type);
        Register target = getMinimumConvertTarget(convert, result.type);
        Register source = getMinimumConvertSource(convert, operand1.type);
        instructions.add(new Instruction(convert, new Address(target), new Address(source)));
        addTemporary(instructions, tempStack);
    }
    private void createMovSB(List<Instruction> instructions, int immediate){
        instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX), new Immediate(immediate)));
        instructions.add(new Instruction(Operation.REP, new Address(Register.MOVSB), null));

    }
    private void assignStruct(List<Instruction> instructions, SymbolTable symbolTable,TemporaryVariableStack tempStack){
        popIntoRegister(instructions, tempStack, new Address(Register.RDI));
        popIntoRegister(instructions, tempStack, new Address(Register.RSI));
        int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), result.type);
        this.createMovSB(instructions, structSize);
    }

    private void moveStruct(List<Instruction> instructions, SymbolTable symbolTable,TemporaryVariableStack tempStack){

        popIntoRegister(instructions, tempStack, new Address(Register.RSI));
        ArgumentSymbol argumentSymbol = (ArgumentSymbol) operand2;
        instructions.add(new Instruction(Operation.LEA, new Address(Register.RDI), new Address(Register.RSP, true, argumentSymbol.getOffset())));

        int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), operand1.type);
        this.createMovSB(instructions, structSize);

    }

    private static final Instruction[] EPILOGUE_INSTRUCTIONS = new Instruction[]{
            new Instruction(Operation.MOV, new Address(Register.RSP), new Address(Register.RBP)),
            new Instruction(Operation.POP, new Address(Register.RBP), null)
    };
    private Operation getConvertOpFromType(DataType source, DataType type){
        if(type.isFloat() && source.isInteger()){
            return Operation.CVTSI2SS;
        }
        if(type.isDouble() && source.isInteger()){
            return Operation.CVTSI2SD;
        }
        if((type.isInteger() || type.isPointer()) && source.isFloat()){
            return Operation.CVTSS2SI;
        }
        if(type.isInteger() && source.isDouble()){
            return Operation.CVTTSD2SI;
        }
        if(type.isFloat() && source.isDouble()){
            return Operation.CVTSD2SS;
        }
        if(type.isDouble() && source.isFloat()){
            return Operation.CVTSS2SD;
        }
        return Operation.MOVSX;
    }
    private Operation getCmpOpFromType(DataType type){
        if(!type.isFloatingPoint()){
            return Operation.CMP;
        }else if(type.isFloat()) {
            return Operation.COMISS;
        }
        return Operation.COMISD;
    }
    private Operation getMoveOpFromType(DataType type){
        if(!type.isFloatingPoint()){
            return Operation.MOV;
        }else if(type.isFloat()) {
            return Operation.MOVSS;
        }
        return Operation.MOVSD;
    }
    private void createShift(List<Instruction> instructions, TemporaryVariableStack tempStack, Operation op) throws CompileException {
        this.popTemporaryIntoSecondary(instructions, tempStack);
        Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
        instructions.add(new Instruction(op, primary, new Address(Register.CL)));
        this.addTemporary(instructions, tempStack);
    }

    private void createBinary(List<Instruction> instructions, TemporaryVariableStack tempStack, Operation op) throws CompileException {
        Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
        Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
        instructions.add(new Instruction(op, primary, secondary));
        this.addTemporary(instructions, tempStack);
    }
    private void createCompare(List<Instruction> instructions, TemporaryVariableStack tempStack, Operation op) throws CompileException {
        Address right = popTemporaryIntoSecondary(instructions, tempStack);
        Address left = popTemporaryIntoPrimary(instructions, tempStack);
        instructions.add(new Instruction(getCmpOpFromType(operand1.type), left, right));
        instructions.add(new Instruction(op, new Address(Register.AL), null));
        instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
        this.addTemporary(instructions, tempStack);
    }

    private Address popIntoRegister(List<Instruction> instructions, TemporaryVariableStack tempStack, Address primary){
        VariableSymbol value            = tempStack.popVariable();
        Operation moveOp                = getMoveOpFromType(value.type);
        instructions.add(new Instruction(moveOp, primary, new Address(Register.RBP, true, value.offset)));
        return primary;

    }
    private Address popTemporaryIntoSecondary(List<Instruction> instructions, TemporaryVariableStack tempStack){
        VariableSymbol value            = tempStack.peek();
        Address primary                 = new Address(Register.getSecondaryRegisterFromDataType(value.type));
        Address out = popIntoRegister(instructions, tempStack, new Address(primary.getRegister()));
        if(value.type.isInteger() && !value.type.isLong()){
            instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RCX), primary));
        }
        return out;
    }
    private Address popTemporaryIntoPrimary(List<Instruction> instructions, TemporaryVariableStack tempStack){
        VariableSymbol value            = tempStack.peek();
        Address primary                 = new Address(Register.getPrimaryRegisterFromDataType(value.type));
        Address out = popIntoRegister(instructions, tempStack, new Address(primary.getRegister()));
        if(value.type.isInteger() && !value.type.isLong()){
            instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), primary));
        }
        return out;
    }
    private void addTemporary(List<Instruction> instructions, TemporaryVariableStack tempStack) throws CompileException {
        int offset;
        if(result.type.isStruct()){
            offset = tempStack.pushVariable(result.name, DataType.getPointerFromType(result.type));
        }else{
            offset = tempStack.pushVariable(result.name, result.type);
        }
        Operation moveOp = getMoveOpFromType(result.type);
        instructions.add(new Instruction(moveOp, new Address(Register.RBP, true, offset), new Address(Register.getPrimaryRegisterFromDataType(result.type))));
    }

    private static final Register[] LINUX_FLOAT_PARAM_LOCATIONS = new Register[]{Register.XMM0, Register.XMM1, Register.XMM2, Register.XMM3, Register.XMM4, Register.XMM5};
    private static final Register[] LINUX_GENERAL_PARAM_LOCATIONS = new Register[]{Register.RDI, Register.RSI, Register.RDX, Register.RCX, Register.R8, Register.R9};

    private void addExternalParameter(List<Instruction> instructions, TemporaryVariableStack tempStack, ArgumentSymbol argumentSymbol) throws CompileException {

        VariableSymbol param = tempStack.peek();
        popTemporaryIntoPrimary(instructions, tempStack);
        Register[] registers = operand1.type.isFloatingPoint() ? LINUX_FLOAT_PARAM_LOCATIONS : LINUX_GENERAL_PARAM_LOCATIONS;
        if(argumentSymbol.getCount() >= registers.length){
            // Push it onto the stack
            throw new CompileException("Pls fix :)");
        }else{
            Address target = new Address(registers[argumentSymbol.getCount()]);
            Register source = param.type.isFloatingPoint() ? Register.PRIMARY_SSE_REGISTER : Register.PRIMARY_GENERAL_REGISTER;
            instructions.add(new Instruction(getMoveOpFromType(param.type), target, new Address(source)));
            // move it into the register
        }
    }
    private void createLogical(SymbolTable symbolTable, List<Instruction> instructions, TemporaryVariableStack tempStack, int firstImmediate) throws CompileException {
        Address right = popTemporaryIntoSecondary(instructions, tempStack);
        Address left = popTemporaryIntoPrimary(instructions, tempStack);
        instructions.add(new Instruction(Operation.CMP, left, new Immediate(firstImmediate)));
        String mergeLabel        = symbolTable.generateLabel().name;

        instructions.add(new Instruction(Operation.JE, new Address(mergeLabel, false), null));
        instructions.add(new Instruction(Operation.CMP, right, new Immediate(1)));
        instructions.add(new Instruction(Operation.SETE, new Address(Register.AL), null));
        instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
        instructions.add(new Instruction(mergeLabel));
        addTemporary(instructions, tempStack);
    }
    private void calculateIndex(SymbolTable symbolTable, List<Instruction> instructions, TemporaryVariableStack tempStack) throws CompileException {
        popTemporaryIntoPrimary(instructions, tempStack);
        popTemporaryIntoSecondary(instructions, tempStack);

        int size = symbolTable.getStructSize(operand1.type.getTypeFromPointer());
        instructions.add(new Instruction(Operation.IMUL, new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate(size)));
        instructions.add(new Instruction(Operation.ADD, new Address(Register.PRIMARY_GENERAL_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
    }

    private void createJumpOnCondition(List<Instruction> instructions, TemporaryVariableStack tempStack){
        Immediate immediate = new Immediate(this.op == QuadOp.JMP_F ? 0 : 1);
        popTemporaryIntoPrimary(instructions, tempStack);
        instructions.add(new Instruction(getCmpOpFromType(operand1.type), new Address(Register.PRIMARY_GENERAL_REGISTER), immediate));
        instructions.add(new Instruction(Operation.JE, new Address(operand1.name, false), null));
    }

    public List<Instruction> emitInstructions(SymbolTable symbolTable, Map<String, Constant> constants,TemporaryVariableStack tempStack) throws CompileException{
        List<Instruction> instructions = new ArrayList<>();
        switch(op){
            case ALLOCATE:{
                instructions.add(new Instruction(Operation.SUB, new Address(Register.RSP), new Immediate(operand1.name)));
                break;
            }
            case LOGICAL_AND, LOGICAL_OR:{
                this.createLogical(symbolTable, instructions, tempStack, this.op == QuadOp.LOGICAL_OR ? 1 : 0);
                break;
            }
            case PARAM:{
                if(operand1.type.isStruct()){
                    this.moveStruct(instructions, symbolTable, tempStack);
                }else {
                    ArgumentSymbol argumentSymbol = (ArgumentSymbol) operand2;
                    if(!argumentSymbol.getExternal()){
                        popTemporaryIntoPrimary(instructions, tempStack);
                        instructions.add(new Instruction(getMoveOpFromType(operand1.type), new Address(Register.RSP, true, argumentSymbol.getOffset()), new Address(Register.getPrimaryRegisterFromDataType(operand1.type))));
                    }else{
                        this.addExternalParameter(instructions, tempStack, argumentSymbol);
                    }
                }
                break;
            }
            case CALL:{
                instructions.add(new Instruction(Operation.CALL, new Immediate(operand1.name), null));
                // Hoist
                int size = symbolTable.getFunctionStackSizeAlignment(operand1.name);
                if(size != 0){
                    instructions.add(new Instruction(Operation.ADD, new Address(Register.RSP), new Immediate(size)));
                }
                if(!result.type.isVoid()){
                   addTemporary(instructions, tempStack);
                }
                break;
            }
            case JMP :{
                instructions.add(new Instruction(Operation.JMP, new Immediate(operand1.name), null));
                break;
            }
            case LOAD, LOAD_POINTER:{
                VariableSymbol variable = (VariableSymbol) operand1;
                Operation op = (operand1.type.isStruct() || this.op == QuadOp.LOAD_POINTER) ? Operation.LEA : getMoveOpFromType(operand1.type);
                instructions.add(new Instruction(op, new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.RBP, true, variable.offset)));
                addTemporary(instructions, tempStack);
                break;
            }
            case CONVERT:{
                this.convertOperand(instructions,tempStack);
                break;
            }
            case LOAD_MEMBER, LOAD_MEMBER_POINTER:{
                ImmediateSymbol memberSymbol    = (ImmediateSymbol) operand2;
                popTemporaryIntoPrimary(instructions, tempStack);
                Operation operation = (result.type.isStruct() || this.op == QuadOp.LOAD_MEMBER_POINTER) ? Operation.LEA : getMoveOpFromType(result.type);
                instructions.add(new Instruction(operation, new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.RAX, true, Integer.parseInt(memberSymbol.getValue()))));
                addTemporary(instructions, tempStack);
                break;
            }
            case STORE_ARRAY_ITEM:{
                VariableSymbol arraySymbol = (VariableSymbol) operand1;
                ArrayDataType arrayDataType = (ArrayDataType)arraySymbol.type;
                String index = ((ImmediateSymbol)operand2).getValue();
                int offset = arraySymbol.offset + Integer.parseInt(index);
                if(arrayDataType.itemType.isStruct()){
                    popIntoRegister(instructions, tempStack, new Address(Register.RSI));
                    instructions.add(new Instruction(Operation.LEA, new Address(Register.RDI), new Address(Register.RBP, true, offset)));

                    this.createMovSB(instructions, SymbolTable.getStructSize(symbolTable.getStructs(), arrayDataType.itemType));
                    break;
                }

                popTemporaryIntoPrimary(instructions, tempStack);
                Operation moveOp = getMoveOpFromType(arrayDataType.itemType);
                instructions.add(new Instruction(moveOp, new Address(Register.RBP, true, offset), new Address(Register.getPrimaryRegisterFromDataType(arrayDataType.itemType))));
                break;
            }
            case REFERENCE_INDEX, INDEX:{
                this.calculateIndex(symbolTable, instructions, tempStack);
                if(this.op == QuadOp.INDEX){
                    Operation operation = result.type.isStruct() ? Operation.LEA : getMoveOpFromType(result.type);
                    instructions.add(new Instruction(operation, new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.PRIMARY_GENERAL_REGISTER, true)));
                }
                addTemporary(instructions, tempStack);
                break;
            }
            case DEREFERENCE:{
                Address primary = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(getMoveOpFromType(result.type), new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(primary.getRegister(), true)));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case JMP_T, JMP_F: {
                this.createJumpOnCondition(instructions, tempStack);
                break;
            }
            case PRE_INC_F:{
                setupPostfixFloat(tempStack, instructions);
                this.immediateArithmeticFloat(instructions, result.type.isDouble() ? Operation.ADDSD : Operation.ADDSS);
                addTemporary(instructions, tempStack);
                break;
            }
            case PRE_DEC_F:{
                setupPostfixFloat(tempStack, instructions);
                this.immediateArithmeticFloat(instructions, result.type.isDouble() ? Operation.SUBSD : Operation.SUBSS);
                addTemporary(instructions, tempStack);
                break;
            }
            case POST_INC_F:{
                setupPostfixFloat(tempStack, instructions);
                addTemporary(instructions, tempStack);
                this.immediateArithmeticFloat(instructions, result.type.isDouble() ? Operation.ADDSD : Operation.ADDSS);
                break;
            }
            case POST_DEC_F:{
                setupPostfixFloat(tempStack, instructions);
                addTemporary(instructions, tempStack);
                this.immediateArithmeticFloat(instructions, result.type.isDouble() ? Operation.SUBSD : Operation.SUBSS);
                break;
            }
            case PRE_INC_I:{
                this.createPostfixInteger(instructions, tempStack, Operation.INC, Operation.ADD, false);
                break;
            }
            case PRE_DEC_I:{
                this.createPostfixInteger(instructions, tempStack, Operation.DEC, Operation.SUB, false);
                break;
            }
            case POST_INC_I:{
                this.createPostfixInteger(instructions, tempStack, Operation.INC, Operation.ADD, true);
                break;
            }
            case POST_DEC_I:{
                this.createPostfixInteger(instructions, tempStack, Operation.DEC, Operation.SUB, true);
                break;
            }
            case ADD_I:{
                this.createBinary(instructions, tempStack, Operation.ADD);
                break;
            }
            case SUB_I:{
                this.createBinary(instructions, tempStack, Operation.SUB);
                break;
            }
            case MUL_I:{
                this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.MUL, secondary, null));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case DIV_I:{
                Address secondary = this.popTemporaryIntoSecondary(instructions, tempStack);
                this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.XOR, new Address(Register.RDX), new Address(Register.RDX)));
                instructions.add(new Instruction(Operation.IDIV, secondary, null));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case SHL, SHR:{
                this.createShift(instructions, tempStack, this.op == QuadOp.SHL ? Operation.SHL : Operation.SHR);
                break;
            }
            case ADD_F:{
                this.createBinary(instructions, tempStack, result.type.isFloat() ? Operation.ADDSS : Operation.ADDSD);
                break;
            }
            case SUB_F:{
                this.createBinary(instructions, tempStack, result.type.isFloat() ? Operation.SUBSS : Operation.SUBSD);
                break;
            }
            case MUL_F:{
                this.createBinary(instructions, tempStack, result.type.isFloat() ? Operation.MULSS : Operation.MULSD);
                break;
            }
            case DIV_F:{
                this.createBinary(instructions, tempStack, result.type.isFloat() ? Operation.DIVSS : Operation.DIVSD);
                break;
            }
            case NEGATE:{
                Address primary = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.NOT, primary, null));
                instructions.add(new Instruction(Operation.INC, primary, null));
                addTemporary(instructions, tempStack);
                break;
            }
            case AND:{
                this.createBinary(instructions, tempStack, Operation.AND);
                break;
            }
            case OR:{
                this.createBinary(instructions, tempStack, Operation.OR);
                break;
            }
            case XOR:{
                this.createBinary(instructions, tempStack, Operation.XOR);
                break;
            }
            case LESS_I:{
                this.createCompare(instructions, tempStack, Operation.SETL);
                break;
            }
            case LESS_EQUAL_I:{
                this.createCompare(instructions, tempStack, Operation.SETLE);
                break;
            }
            case GREATER_I:{
                this.createCompare(instructions, tempStack, Operation.SETG);
                break;
            }
            case GREATER_EQUAL_I:{
                this.createCompare(instructions, tempStack, Operation.SETGE);
                break;
            }
            case EQUAL_I, EQUAL_F:{
                this.createCompare(instructions, tempStack, Operation.SETE);
                break;
            }
            case NOT_EQUAL_I, NOT_EQUAL_F:{
                this.createCompare(instructions, tempStack, Operation.SETNE);
                break;
            }
            case NOT_I :{
                Address primary = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.NOT, primary, new Immediate(1)));
                addTemporary(instructions, tempStack);
                break;
            }
            case MOD:{
                instructions.add(new Instruction(Operation.XOR, new Address(Register.RDX), new Address(Register.RDX)));
                Address secondary = popIntoRegister(instructions,  tempStack, new Address(Register.getSecondaryRegisterFromDataType(operand2.type)));
                popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.IDIV, secondary, null));
                instructions.add(new Instruction(getMoveOpFromType(result.type), new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.getThirdRegisterFromDataType(result.type))));
                addTemporary(instructions, tempStack);
                break;
            }
            case LESS_F:{
                this.createCompare(instructions, tempStack, Operation.SETB);
                break;
            }
            case LESS_EQUAL_F:{
                this.createCompare(instructions, tempStack, Operation.SETBE);
                break;
            }
            case GREATER_F:{
                this.createCompare(instructions, tempStack, Operation.SETA);
                break;
            }
            case GREATER_EQUAL_F:{
                this.createCompare(instructions, tempStack, Operation.SETAE);
                break;
            }
            case LABEL:{
                instructions.add(new Instruction(operand1.name));
                break;
            }
            case RET_I, RET_F:{
                if(result != null){
                    this.popTemporaryIntoPrimary(instructions, tempStack);
                }
                instructions.addAll(List.of(EPILOGUE_INSTRUCTIONS));
                instructions.add(new Instruction(Operation.RET, null, null));
                break;
            }
            case LOAD_IMM_I, LOAD_IMM_F:{
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                if(result.type.isString() || result.type.isFloatingPoint()) {
                    Constant constant = constants.get(immediateSymbol.getValue());
                    instructions.add(new Instruction(getMoveOpFromType(result.type), new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(constant.label(), !result.type.isString())));
                }else{
                    instructions.add(new Instruction(getMoveOpFromType(result.type), new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Immediate(immediateSymbol.getValue())));
                }
                this.addTemporary(instructions, tempStack);
                break;
            }
            case IMUL:{
                Address primary = popTemporaryIntoPrimary(instructions, tempStack);
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                instructions.add(new Instruction(Operation.IMUL, primary, new Immediate(immediateSymbol.getValue())));
                addTemporary(instructions, tempStack);
                break;
            }
            case CAST:{
               break;
            }
            case ASSIGN:{
                if(operand1.type.isStruct()){
                    assignStruct(instructions, symbolTable, tempStack);
                }else{
                    VariableSymbol pointer  = tempStack.popVariable();
                    VariableSymbol value    = tempStack.popVariable();
                    instructions.add(new Instruction(getMoveOpFromType(value.type), new Address(Register.getPrimaryRegisterFromDataType(value.type)), new Address(Register.RBP, true, value.offset)));
                    instructions.add(new Instruction(getMoveOpFromType(pointer.type), new Address(Register.getSecondaryRegisterFromDataType(pointer.type)), new Address(Register.RBP, true, pointer.offset)));
                    instructions.add(new Instruction(getMoveOpFromType(operand1.type), new Address(Register.RCX, true), new Address(Register.getPrimaryRegisterFromDataType(operand1.type))));
                }

                break;
            }
            default : {throw new CompileException(String.format("Don't know how to make instructions from %s", op.name()));}
        }
        return instructions;
    }

    private void setupPostfixFloat(TemporaryVariableStack tempStack, List<Instruction> instructions) {
        popTemporaryIntoPrimary(instructions, tempStack);

        Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
        Operation moveOp = getMoveOpFromType(result.type);
        instructions.add(new Instruction(moveOp, primary, new Address(Register.RAX, true)));

        instructions.add(new Instruction(Operation.MOV, new Address(Register.SECONDARY_GENERAL_REGISTER), new Immediate("1")));
        Operation convertOp = result.type.isDouble() ? Operation.CVTSI2SD : Operation.CVTSI2SS;
        instructions.add(new Instruction(convertOp, new Address(Register.SECONDARY_SSE_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
    }
    private void immediateArithmeticFloat(List<Instruction> instructions,  Operation arithmeticOp){
        Operation moveOp = getMoveOpFromType(result.type);
        instructions.add(new Instruction(arithmeticOp, new Address(Register.PRIMARY_SSE_REGISTER), new Address(Register.SECONDARY_SSE_REGISTER)));
        instructions.add(new Instruction(moveOp, new Address(Register.RAX, true), new Address(Register.PRIMARY_SSE_REGISTER)));
    }
    private void createPostfixInteger(List<Instruction> instructions, TemporaryVariableStack tempStack, Operation op,Operation pointerOp, boolean post) throws CompileException {
        this.popTemporaryIntoSecondary(instructions, tempStack);
        Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
        instructions.add(new Instruction(Operation.MOV, primary, new Address(Register.RCX, true)));
        if(post){
            this.addTemporary(instructions, tempStack);
        }
        if(operand1.type.isPointer()){
            instructions.add(new Instruction(pointerOp, primary, new Immediate(operand1.type.getTypeFromPointer().getSize())));
        }else{
            instructions.add(new Instruction(op, primary, null));
        }
        instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX, true), primary));

        if(!post){
            this.addTemporary(instructions, tempStack);
        }
    }
}
