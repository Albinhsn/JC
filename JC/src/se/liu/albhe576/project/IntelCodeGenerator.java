package se.liu.albhe576.project;

import java.util.*;

public class IntelCodeGenerator implements CodeGenerator{
    public static final Register[] LINUX_FLOAT_PARAM_LOCATIONS = new Register[]{Register.XMM0, Register.XMM1, Register.XMM2, Register.XMM3, Register.XMM4, Register.XMM5, Register.XMM6, Register.XMM7};
    public static final Register[] LINUX_GENERAL_PARAM_LOCATIONS = new Register[]{Register.RDI, Register.RSI, Register.RDX, Register.RCX, Register.R8, Register.R9};
    private final SymbolTable symbolTable;
    private QuadList quads;
    private List<Instruction> instructions;
    private int index;
    private TemporaryVariableStack temporaryStack;
    private boolean isAtEnd(){
        return index >= this.quads.size();
    }
    private DataType getCurrentResult() throws CompileException {
        return this.getCurrentQuad().result().type;
    }
    private DataType getCurrentDestination() throws CompileException {
        return this.getCurrentQuad().operand1().type;
    }
    private Symbol getCurrentDestinationSymbol() throws CompileException {
        return this.getCurrentQuad().operand1();
    }
    private QuadOp getCurrentOp() throws CompileException {
        return this.getCurrentQuad().op();
    }
    private Quad getCurrentQuad() throws CompileException{
        Quad out = this.quads.get(index);
        if(out == null){
            throw new CompileException("Can't get current at end?");
        }
        return out;
    }
    private void advance(){this.index++;}
    public List<Instruction> generateInstructions(QuadList quads, String name) throws CompileException {
        this.index              = 0;
        this.quads              = quads;
        this.instructions       = new ArrayList<>();
        this.temporaryStack     = new TemporaryVariableStack(symbolTable, name);

        this.addPrologue();
        int allocateStackSpaceIndex = this.instructions.size();
        while(!this.isAtEnd()){
            if(!this.optimize()){
                this.INSTRUCTION_GENERATION_MAP.get(this.getCurrentQuad().op()).generate();
            }
            advance();
        }

        int stackSpace  = -temporaryStack.getMaxOffset();
        stackSpace += Compiler.getStackAlignment(stackSpace);
        if(stackSpace != 0){
            this.instructions.add(allocateStackSpaceIndex, new IntelInstruction(new Operation(OperationType.SUB), new Address<>(Register.RSP), new Address<>(stackSpace)));
        }


        if(!this.instructions.get(this.instructions.size() - 1).isRet()){
            this.addEpilogue();
            this.instructions.add(new IntelInstruction(new Operation(OperationType.RET)));
        }
        return this.instructions;
    }
    @FunctionalInterface private interface Generate{ void generate() throws CompileException;}
    private final Map<QuadOp, Generate> INSTRUCTION_GENERATION_MAP = Map.ofEntries(
            Map.entry(QuadOp.RET, this::createReturn),
            Map.entry(QuadOp.LABEL, this::createLabel),
            Map.entry(QuadOp.JMP, this::createJmp),
            Map.entry(QuadOp.LOAD_MEMBER_POINTER, this::createLoadMember),
            Map.entry(QuadOp.LOAD_POINTER, this::createLoad),
            Map.entry(QuadOp.CONVERT, this::createConvert),
            Map.entry(QuadOp.ARGUMENT, this::createArgument),
            Map.entry(QuadOp.LOAD_MEMBER, this::createLoadMember),
            Map.entry(QuadOp.INDEX, this::createIndex),
            Map.entry(QuadOp.DEREFERENCE, this::createDereference),
            Map.entry(QuadOp.JMP_T, this::createJumpOnCondition),
            Map.entry(QuadOp.JMP_F, this::createJumpOnCondition),
            Map.entry(QuadOp.ALLOCATE, this::createAllocate),
            Map.entry(QuadOp.REFERENCE_INDEX, this::createIndex),
            Map.entry(QuadOp.ASSIGN, this::createAssign),
            Map.entry(QuadOp.IMUL, this::createIMul),
            Map.entry(QuadOp.CAST, this::createCast),
            Map.entry(QuadOp.ADD, this::createBinary),
            Map.entry(QuadOp.SUB, this::createBinary),
            Map.entry(QuadOp.MUL, this::createBinary),
            Map.entry(QuadOp.DIV, this::createBinary),
            Map.entry(QuadOp.SHL, this::createShift),
            Map.entry(QuadOp.SHR, this::createShift),
            Map.entry(QuadOp.MOD, this::createMod),
            Map.entry(QuadOp.PRE_INC, this::createInc),
            Map.entry(QuadOp.PRE_DEC, this::createInc),
            Map.entry(QuadOp.POST_INC, this::createInc),
            Map.entry(QuadOp.POST_DEC, this::createInc),
            Map.entry(QuadOp.NEGATE, this::createNegate),
            Map.entry(QuadOp.AND, this::createBinary),
            Map.entry(QuadOp.OR, this::createBinary),
            Map.entry(QuadOp.XOR, this::createBinary),
            Map.entry(QuadOp.LOGICAL_OR, this::createLogical),
            Map.entry(QuadOp.LOGICAL_AND, this::createLogical),
            Map.entry(QuadOp.LESS, this::createComparison),
            Map.entry(QuadOp.LESS_EQUAL, this::createComparison),
            Map.entry(QuadOp.GREATER, this::createComparison),
            Map.entry(QuadOp.GREATER_EQUAL, this::createComparison),
            Map.entry(QuadOp.EQUAL, this::createComparison),
            Map.entry(QuadOp.NOT_EQUAL, this::createComparison),
            Map.entry(QuadOp.NOT, this::createNot),
            Map.entry(QuadOp.CALL, this::createCall),
            Map.entry(QuadOp.LOAD_IMM, this::createLoadImmediate),
            Map.entry(QuadOp.LOAD, this::createLoad),
            Map.entry(QuadOp.STORE_ARRAY_ITEM, this::createStoreArrayItem)
    );
    private void addInstruction(OperationType op, Register destRegister, Register sourceRegister){
        Address<?> dest     = destRegister   == null ? null : new Address<>(destRegister);
        Address<?> source   = sourceRegister == null ? null : new Address<>(sourceRegister);
        this.instructions.add(new IntelInstruction(new Operation(op), dest, source));
    }
    private void addInstruction(OperationType op, Operand dest, Operand source){
        this.instructions.add(new IntelInstruction(new Operation(op), dest, source));
    }
    public void addTemporary() throws CompileException {
        DataType type           = this.getCurrentResult();
        type                    = type.isStruct() ? type.getPointerFromType() : type;
        int offset              = this.temporaryStack.pushVariable(type);
        OperationType moveOp    = Operation.getMoveOpFromType(type);
        this.addInstruction(moveOp, new Address<>(Register.RBP, true, offset), new Address<>(Register.getPrimaryRegisterFromDataType(type)));
    }
    public Register popTemporaryIntoSecondary(){
        TemporaryStackVariable value    = this.temporaryStack.peekVariable();
        Register secondary              = Register.getSecondaryRegisterFromDataType(value.type());
        popIntoRegister(secondary);
        if(value.type().isInteger() && !(value.type().isLong() || value.type().isInt())){
            this.createSignExtend(Register.SECONDARY_GENERAL_REGISTER, secondary);
        }
        return secondary;
    }
    public OperationType popIntoRegister(Register primary){
        TemporaryStackVariable variable = this.temporaryStack.popVariable();
        Operation moveOp                = new Operation(Operation.getMoveOpFromType(variable.type()));
        this.instructions.add(new IntelInstruction(moveOp, new Address<>(primary), new Address<>(Register.RBP, true, variable.offset())));
        return moveOp.getType();
    }
    private void createSignExtend(Register dest, Register source){
        this.instructions.add(new IntelInstruction(new Operation(OperationType.MOVSX), new Address<>(dest), new Address<>(source)));
    }
    public Register popTemporaryIntoPrimary(){
        TemporaryStackVariable variable = this.temporaryStack.peekVariable();
        Register primary                = Register.getPrimaryRegisterFromDataType(variable.type());
        OperationType lastOp            = this.popIntoRegister(primary);
        if(lastOp != OperationType.MOVSX && variable.type().isInteger() && !(variable.type().isLong() || variable.type().isInt())){
            this.createSignExtend(Register.RAX, primary);
        }
        return primary;
    }
    private void createBinary() throws CompileException {
        QuadOp currentOp     = this.getCurrentOp();
        DataType result      = this.getCurrentResult();
        Register secondary   = this.popTemporaryIntoSecondary();
        Register primary     = this.popTemporaryIntoPrimary();

        if(currentOp == QuadOp.DIV && result.isInteger()){
            this.createIntegerDivision();
        } else if(currentOp == QuadOp.MUL && result.isInteger()){
            this.addInstruction(OperationType.MUL, secondary, null);
        } else{
            OperationType op = OperationType.getOpFromResultType(currentOp, result);
            this.addInstruction(op, primary, secondary);
        }
        this.addTemporary();
    }
    private void createLabel() throws CompileException {
        this.instructions.add(new IntelInstruction(new Label(this.getCurrentDestinationSymbol().name)));
    }
    private void createJmp() throws CompileException {
        this.instructions.add(new IntelInstruction(new Operation(OperationType.JMP), new Address<>(this.getCurrentDestinationSymbol().name)));
    }
    private void createCast() {}
    private void createShift() throws CompileException {
        OperationType op    = OperationType.getBinaryOpFromQuadOp(this.getCurrentQuad().op());
        this.popTemporaryIntoSecondary();
        Register primary     = this.popTemporaryIntoPrimary();
        this.addInstruction(op, primary, Register.CL);
        this.addTemporary();
    }
    private void createIntegerDivision(){
        this.addInstruction(OperationType.XOR, new Address<>(Register.RDX), new Address<>(Register.RDX));
        this.addInstruction(OperationType.IDIV, new Address<>(Register.SECONDARY_GENERAL_REGISTER), null);
    }
    private void createMod() throws CompileException {
        DataType type = this.getCurrentResult();
        this.popTemporaryIntoSecondary();
        this.popTemporaryIntoPrimary();
        this.createIntegerDivision();
        this.createMovePrimary(type, new Address<>(Register.getThirdRegisterFromDataType(type)));
        this.addTemporary();
    }
    private void createPostfixFloat(OperationType op, DataType result, boolean post) throws CompileException {
        this.setupPostfixFloat(result);
        op = op.convertToFloatingPoint(result);
        if(post){
            this.addTemporary();
            this.immediateArithmeticFloat(op, result);
        }else{
            this.immediateArithmeticFloat(op, result);
            this.addTemporary();
        }
    }
    private void createInc() throws CompileException {
        Quad current = this.getCurrentQuad();
        DataType result = this.getCurrentResult();
        OperationType op    = OperationType.getBinaryOpFromQuadOp(current.op());
        boolean post = current.op() == QuadOp.POST_INC;
        if(result.isFloatingPoint()){
            this.createPostfixFloat(op, result, post);
        }else{
            OperationType pointerOp = op == OperationType.INC ? OperationType.ADD : OperationType.SUB;
            this.createPostfixInt(op, pointerOp, post, result);
        }
    }
    public void immediateArithmeticFloat(OperationType arithmeticOp, DataType result){
        OperationType moveOp = Operation.getMoveOpFromType(result);
        this.addInstruction(arithmeticOp, Register.PRIMARY_SSE_REGISTER, Register.SECONDARY_SSE_REGISTER);
        this.addInstruction(moveOp, new Address<>(Register.RAX, true), new Address<>(Register.PRIMARY_SSE_REGISTER));
    }
    public void createPostfixInt(OperationType op, OperationType pointerOp, boolean post, DataType target) throws CompileException {
        this.popTemporaryIntoSecondary();
        this.createMovePrimary(target, new Address<>(Register.RCX, true));

        if(post){
            this.addTemporary();
        }

        Address<?> primary = new Address<>(Register.getPrimaryRegisterFromDataType(target));
        if(target.isPointer()){
            int pointerSize = target.getTypeFromPointer().getSize();
            this.addInstruction(pointerOp, primary, new Address<>(pointerSize));
        }else{
            this.addInstruction(op, primary, null);
        }
        this.createMove(DataType.getInt(), new Address<>(Register.RCX, true), primary);

        if(!post){
            this.addTemporary();
        }
    }
    public void setupPostfixFloat(DataType result) {
        this.popTemporaryIntoPrimary();
        this.createMovePrimary(result, new Address<>(Register.RAX, true));
        this.createMove(DataType.getInt(), new Address<>(Register.SECONDARY_GENERAL_REGISTER), new Address<>(1));

        OperationType convertOp = result.isDouble() ? OperationType.CVTSI2SD : OperationType.CVTSI2SS;
        this.addInstruction(convertOp, Register.SECONDARY_SSE_REGISTER, Register.SECONDARY_GENERAL_REGISTER);
    }
    private void createNegate() throws CompileException {
        Register primary    = this.popTemporaryIntoPrimary();
        this.addInstruction(OperationType.NOT, primary, null);
        this.addInstruction(OperationType.INC, primary, null);
        this.addTemporary();
    }

    public void createCompare(OperationType compareOp, OperationType setOp, Register left, Address<?> right){
        this.addInstruction(compareOp, new Address<>(left), right);
        this.addInstruction(setOp, Register.AL, null);
    }
    private void createLabel(String label){
        this.instructions.add(new IntelInstruction(new Label(label), null, null));
    }
    private void createLogical() throws CompileException {
        Register right  = popTemporaryIntoSecondary();
        Register left   = popTemporaryIntoPrimary();
        Quad current    = this.getCurrentQuad();

        int firstRegister = current.op() == QuadOp.LOGICAL_OR ? 1 : 0;

        this.addInstruction(OperationType.CMP, new Address<>(left), new Address<>(firstRegister));
        String mergeLabel        = symbolTable.generateLabel().name;

        this.addInstruction(OperationType.JE, new Address<>(mergeLabel), null);
        this.createCompare(OperationType.CMP, OperationType.SETE, right, new Address<>(1));
        this.createLabel(mergeLabel);
        this.addTemporary();
    }
    private void createComparison() throws CompileException {
        Register right      = popTemporaryIntoSecondary();
        Register left       = popTemporaryIntoPrimary();
        OperationType op    = OperationType.getOpFromResultType(this.getCurrentOp(), this.getCurrentResult());
        DataType destination = this.getCurrentDestination();

        if(destination.isFloatingPoint()){
            op = op.convertToFloat();
        }

        OperationType cmpOp = OperationType.getCmpOpFromType(destination);
        createCompare(cmpOp, op, left, new Address<>(right));
        this.addTemporary();
    }
    private void createNotFloat() throws CompileException {
        Register primary        = this.popTemporaryIntoPrimary();
        this.createMovePrimary(DataType.getInt(), new Address<>(0));

        DataType resultType     = this.getCurrentResult();
        OperationType op        = OperationType.getCmpOpFromType(resultType);
        OperationType convertOp = OperationType.getConvertOpFromType(DataType.getInt(), resultType);

        this.addInstruction(convertOp, Register.SECONDARY_SSE_REGISTER, Register.RAX);
        this.createCompare(op, OperationType.SETE, primary, new Address<>(Register.SECONDARY_SSE_REGISTER));
    }
    private void createNot() throws CompileException {
        if(this.getCurrentResult().isFloatingPoint()){
            this.createNotFloat();
        }else{
            Register primary = this.popTemporaryIntoPrimary();
            this.createCompare(OperationType.CMP, OperationType.SETE, primary, new Address<>(0));
        }
        this.addTemporary();
    }
    public void deallocateStackSpace(int space){
        this.addInstruction(OperationType.ADD, new Address<>(Register.RSP), new Address<>(space));
    }
    private void createCall() throws CompileException {
        Quad current = this.getCurrentQuad();

        ImmediateSymbol stackSizeImmediate = (ImmediateSymbol) current.operand2();
        this.addInstruction(OperationType.CALL, new Address<>(current.operand1().name), null);

        int stackSize    = Integer.parseInt(stackSizeImmediate.getValue());
        stackSize       += Compiler.getStackAlignment(stackSize);
        if(stackSize != 0){
            this.deallocateStackSpace(stackSize);
        }

        if(!current.result().type.isVoid()){
            this.addTemporary();
        }
    }
    private Address<?> getImmediate(ImmediateSymbol immediateSymbol, DataType result){
        if(result.isString() || result.isFloatingPoint()) {
            Constant constant = symbolTable.getConstants().get(immediateSymbol.getValue());
            return new Address<>(constant.label(), !result.isString());
        }
        return new Address<>(immediateSymbol.getValue());
    }
    private void createMove(DataType destinationType, Address<?> destination, Address<?> source){
        this.addInstruction(OperationType.getMoveOpFromType(destinationType), destination, source);
    }
    private void createMovePrimary(DataType destinationType, Address<?> source){
        this.createMove(destinationType, new Address<>(Register.getPrimaryRegisterFromDataType(destinationType)), source);
    }
    private void createLoadImmediate() throws CompileException {
        DataType result                     = getCurrentResult();
        Address<?> immediate                = getImmediate((ImmediateSymbol) getCurrentDestinationSymbol(), result);

        this.createMovePrimary(result, immediate);
        this.addTemporary();
    }
    private void createLoad() throws CompileException {
        Quad current            = getCurrentQuad();
        VariableSymbol variable = (VariableSymbol) current.operand1();
        DataType result         = getCurrentResult();

        Address<?> source = new Address<>(Register.RBP, true, variable.offset);
        if(variable.type.isStruct() || current.op() == QuadOp.LOAD_POINTER){
            this.addInstruction(OperationType.LEA, new Address<>(Register.getPrimaryRegisterFromDataType(result)), source);
        }else{
            this.createMovePrimary(variable.type, source);
        }
        this.addTemporary();
    }
    private Address<?> getStackVariableAddress(int offset){
        return new Address<>(Register.RBP, true, offset);
    }
    private void createStoreArrayItem() throws CompileException {
        Quad current                = getCurrentQuad();
        ArrayItemSymbol arraySymbol = (ArrayItemSymbol) current.operand1();
        ArrayDataType arrayDataType = (ArrayDataType)   arraySymbol.type;

        Address<?> stackAddress = this.getStackVariableAddress(arraySymbol.offset + arraySymbol.getOffset());
        if(arrayDataType.itemType.isStruct()){
            this.popIntoRegister(Register.RSI);
            this.addInstruction(OperationType.LEA, new Address<>(Register.RDI), stackAddress);
            this.createMovSB(arrayDataType.itemType);
        }else{
            this.popTemporaryIntoPrimary();
            OperationType moveOp = OperationType.getMoveOpFromType(arrayDataType.itemType);
            this.addInstruction(moveOp, stackAddress, new Address<>(Register.getPrimaryRegisterFromDataType(arrayDataType.itemType)));
        }

    }
    private void addPrologue(){
        this.addInstruction(OperationType.PUSH, Register.RBP, null);
        this.addInstruction(OperationType.MOV, Register.RBP, Register.RSP);
    }
    private void addEpilogue(){
        this.addInstruction(OperationType.MOV, Register.RSP, Register.RBP);
        this.addInstruction(OperationType.POP, Register.RBP, null);
    }
    private void createIMul() throws CompileException {
        Quad current                    = this.getCurrentQuad();
        ImmediateSymbol immediateSymbol = (ImmediateSymbol) current.operand2();
        Register primary                = this.popTemporaryIntoPrimary();
        this.addInstruction(OperationType.IMUL, new Address<>(primary), new Address<>(immediateSymbol.getValue()));
        this.addTemporary();
    }
    public void createMovSB(DataType structType){
        int immediate = SymbolTable.getStructSize(symbolTable.getStructs(), structType);
        this.addInstruction(OperationType.MOV, new Address<>(Register.RCX), new Address<>(immediate));
        this.addInstruction(OperationType.REP, new Address<>(Register.MOVSB), null);
    }
    private void createAssign() throws CompileException {
        Quad current        = this.getCurrentQuad();
        DataType targetType = current.operand1().type;
        if(targetType.isStruct()){
            popIntoRegister(Register.RDI);
            popIntoRegister(Register.RSI);
            this.createMovSB(targetType);
        }else{
            TemporaryStackVariable pointer  = this.temporaryStack.popVariable();
            TemporaryStackVariable value    = this.temporaryStack.popVariable();
            this.createMovePrimary(value.type(), this.getStackVariableAddress(value.offset()));
            this.createMove(pointer.type(), new Address<>(Register.getSecondaryRegisterFromDataType(pointer.type())), this.getStackVariableAddress(pointer.offset()));
            this.createMove(targetType, new Address<>(Register.RCX, true), new Address<>(Register.getPrimaryRegisterFromDataType(targetType)));
        }
    }
    private void createAllocate() throws CompileException {
        Symbol immediateSymbol =  getCurrentQuad().operand1();
        this.addInstruction(OperationType.SUB, new Address<>(Register.RSP), new Address<>(immediateSymbol.name));
    }
    private void createJumpOnCondition() throws CompileException {
        Symbol dest             = getCurrentQuad().operand1();
        int jumpOnTrue          = getCurrentQuad().op() == QuadOp.JMP_T ? 1 : 0;
        popTemporaryIntoPrimary();

        OperationType cmpOp = OperationType.getCmpOpFromType(dest.type);
        this.addInstruction(cmpOp, new Address<>(Register.PRIMARY_GENERAL_REGISTER), new Address<>(jumpOnTrue));
        this.addInstruction(OperationType.JE, new Address<>(dest.name), null);
    }
    private void createDereference() throws CompileException {
        DataType result     = this.getCurrentResult();
        Register primary    = this.popTemporaryIntoPrimary();
        this.createMovePrimary(result, new Address<>(primary, true));
        this.addTemporary();
    }
    private void createIndex() throws CompileException {
        Quad current = getCurrentQuad();
        DataType target = current.result().type;

        popTemporaryIntoPrimary();
        popTemporaryIntoSecondary();
        int size = symbolTable.getStructSize(current.operand1().type.getTypeFromPointer());
        this.addInstruction(OperationType.IMUL, new Address<>(Register.PRIMARY_GENERAL_REGISTER), new Address<>(size));
        this.addInstruction(OperationType.ADD, new Address<>(Register.PRIMARY_GENERAL_REGISTER), new Address<>(Register.SECONDARY_GENERAL_REGISTER));

        if(current.op() == QuadOp.INDEX){
            OperationType operationType = target.isStruct() ? OperationType.LEA : OperationType.getMoveOpFromType(target);
            this.addInstruction(operationType, new Address<>(Register.getPrimaryRegisterFromDataType(target)), new Address<>(Register.PRIMARY_GENERAL_REGISTER, true));
        }
        this.addTemporary();
    }
    private void createLoadMember() throws CompileException {
        DataType result             = getCurrentResult();
        MemberSymbol member         = (MemberSymbol) getCurrentDestinationSymbol();
        Address<?> memberAddress    = new Address<>(Register.PRIMARY_GENERAL_REGISTER, true, member.getOffset());

        this.popTemporaryIntoPrimary();
        if(result.isStruct() || getCurrentOp() == QuadOp.LOAD_MEMBER_POINTER){
            this.addInstruction(OperationType.LEA, new Address<>(Register.PRIMARY_GENERAL_REGISTER), memberAddress);
        }else{
            this.createMovePrimary(result, memberAddress);
        }
        this.addTemporary();
    }
    public void addExternalParameter(ArgumentSymbol argumentSymbol) {
        TemporaryStackVariable param    = this.temporaryStack.peekVariable();
        Register[] registers            = argumentSymbol.type.isFloatingPoint() ? LINUX_FLOAT_PARAM_LOCATIONS : LINUX_GENERAL_PARAM_LOCATIONS;
        popTemporaryIntoPrimary();
        if(argumentSymbol.getCount() >= registers.length){
            this.addInstruction(OperationType.getMoveOpFromType(argumentSymbol.type), new Address<>(Register.RSP, true, argumentSymbol.getOffset()), new Address<>(Register.getPrimaryRegisterFromDataType(argumentSymbol.type)));
        }else{
            Register source = param.type().isFloatingPoint() ? Register.PRIMARY_SSE_REGISTER : Register.PRIMARY_GENERAL_REGISTER;
            this.createMove(param.type(), new Address<>(registers[argumentSymbol.getCount()]), new Address<>(source));
        }
    }
    public void moveStruct(ArgumentSymbol argument){
        popIntoRegister(Register.RSI);
        this.addInstruction(OperationType.LEA, new Address<>(Register.RDI), new Address<>(Register.RSP, true, argument.getOffset()));
        this.createMovSB(argument.type);
    }
    private void createArgument() throws CompileException {
        Quad current = this.getCurrentQuad();
        ArgumentSymbol argumentSymbol = (ArgumentSymbol) current.operand1();
        if(current.operand1().type.isStruct()){
            this.moveStruct(argumentSymbol);
        }else if(!argumentSymbol.getExternal()){
            this.popTemporaryIntoPrimary();
            this.createMove(argumentSymbol.type, new Address<>(Register.RSP, true, argumentSymbol.getOffset()), new Address<>(Register.getPrimaryRegisterFromDataType(argumentSymbol.type)));
        }
        else{
            this.addExternalParameter(argumentSymbol);
        }
    }
    private void createConvert() throws CompileException {
        DataType dest     = getCurrentDestination();
        DataType result = getCurrentResult();
        if(dest.isInteger() && result.isInteger() && dest.isSameType(DataType.getHighestDataTypePrecedence(dest, result))){
            return;
        }
        popTemporaryIntoPrimary();

        OperationType convert   = OperationType.getConvertOpFromType(dest, result);
        Register target         = Register.getMinimumConvertTarget(convert, result);
        Register source         = Register.getMinimumConvertSource(convert, dest);
        this.addInstruction(convert, target, source);
        this.addTemporary();
    }

    private void createReturn() throws CompileException {
        if(this.getCurrentQuad().result() != null){
            this.popTemporaryIntoPrimary();
        }
        this.addEpilogue();
        this.instructions.add(new IntelInstruction(new Operation(OperationType.RET)));
    }

    private boolean optimizeAssignment() throws CompileException {
        final int matchCount = 2;
        if(quads.size() - index < matchCount){
            return false;
        }

        Quad current = this.getCurrentQuad();
        if(current.op() != QuadOp.LOAD_POINTER){
            return false;
        }
        index++;
        VariableSymbol variable = (VariableSymbol) current.operand1();
        int offset = variable.offset;

        current = this.getCurrentQuad();
        if(current.op() != QuadOp.ASSIGN || (current.result().type.isStruct())){
            index--;
            return false;
        }
        this.instructions.remove(this.instructions.size() - 1);

        OperationType movOp = OperationType.getMoveOpFromType(getCurrentResult());
        this.addInstruction(movOp, this.getStackVariableAddress(offset), new Address<>(Register.getPrimaryRegisterFromDataType(getCurrentResult())));

        return true;
    }

    private boolean optimize() throws CompileException {
        return optimizeAssignment();
    }

    private void outputConstants(StringBuilder stringBuilder){
        stringBuilder.append("\n\n");
        for(Map.Entry<String, Constant> entry : this.symbolTable.getConstants().entrySet()){
            Constant value = entry.getValue();
            if(value.type() == DataTypes.STRING){
                // Need to replace \n with 10 which is ASCII for new line
                // Should be done for other escape characters as well
                String formatted = entry.getKey().replace("\\n", "\", 10, \"");
                stringBuilder.append(String.format("%s db \"%s\", 0\n", value.label(), formatted));
            }else{
                stringBuilder.append(String.format("%s dd %s\n", value.label(), entry.getKey()));
            }
        }
    }

    private void outputHeader(StringBuilder stringBuilder){
        stringBuilder.append("global _start\n");
    }
    private void outputExternal(StringBuilder stringBuilder){
        Map<String, Function> externalFunctions = symbolTable.getExternalFunctions();
        for(String function : externalFunctions.keySet()){
            stringBuilder.append(String.format("extern %s\n", function));
        }
    }
    private void outputFunction(StringBuilder stringBuilder, String name, List<Instruction> instructions){
        stringBuilder.append(String.format("\n\n%s:\n", name));
        for(Instruction instruction: instructions){
            stringBuilder.append(String.format("%s\n", instruction.emit()));
        }
    }
    private void outputMain(StringBuilder stringBuilder) {
        stringBuilder.append("\n\nsection .text\n_start:\ncall main\nmov rbx, rax\nmov rax, 1\nint 0x80\n");
    }

    public StringBuilder outputInstructions(Map<String, List<Instruction>> functions) {
        StringBuilder stringBuilder = new StringBuilder();

        outputHeader(stringBuilder);
        outputExternal(stringBuilder);
        outputConstants(stringBuilder);
        outputMain(stringBuilder);

        for(Map.Entry<String, List<Instruction>> function : functions.entrySet()){
            String name = function.getKey();
            List<Instruction> instructions= function.getValue();
            outputFunction(stringBuilder, name, instructions);
        }
        return stringBuilder;
    }
    public IntelCodeGenerator(SymbolTable symbolTable){
        this.symbolTable    = symbolTable;
    }
}
