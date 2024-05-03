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
            Generate generate = this.INSTRUCTION_GENERATION_MAP.get(Objects.requireNonNull(this.getCurrentQuad()).op());
            if(generate != null){
                generate.generate();
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
    private void addInstruction(OperationType op, Operand dest, Operand source){
        this.instructions.add(new IntelInstruction(new Operation(op), dest, source));
    }
    public void addTemporary(DataType type) {
        type = type.isStruct() ? type.getPointerFromType() : type;
        int offset = this.temporaryStack.pushVariable(type);
        OperationType moveOp = Operation.getMoveOpFromType(type);
        this.addInstruction(moveOp, new Address<>(Register.RBP, true, offset), new Address<>(Register.getPrimaryRegisterFromDataType(type)));
    }
    public Register popTemporaryIntoSecondary(){
        TemporaryStackVariable value    = this.temporaryStack.peekVariable();
        Register secondary              = Register.getSecondaryRegisterFromDataType(value.type());
        popIntoRegister(secondary);
        if(value.type().isInteger() && !value.type().isLong()){
            this.createSignExtend(Register.RCX, secondary);
        }
        return secondary;
    }
    private void addPrologue(){
        instructions.add(new IntelInstruction(new Operation(OperationType.PUSH), new Address<>(Register.RBP)));
        instructions.add(new IntelInstruction(new Operation(OperationType.MOV), new Address<>(Register.RBP), new Address<>(Register.RSP)));
    }
    public OperationType popIntoRegister(Register primary){
        TemporaryStackVariable variable = this.temporaryStack.popVariable();
        Operation moveOp                = new Operation(Operation.getMoveOpFromType(variable.type()));
        this.instructions.add(new IntelInstruction(moveOp, new Address<>(primary), new Address<>(Register.RBP, true, variable.offset())));
        return moveOp.getType();
    }

    private void createSignExtend(Register dest, Register source){this.instructions.add(new IntelInstruction(new Operation(OperationType.MOVSX), new Address<>(dest), new Address<>(source)));}
    public Register popTemporaryIntoPrimary(){
        TemporaryStackVariable variable = this.temporaryStack.peekVariable();
        Register primary                 = Register.getPrimaryRegisterFromDataType(variable.type());
        OperationType lastOp = this.popIntoRegister(primary);
        // Hoist
        if(lastOp != OperationType.MOVSX && variable.type().isInteger() && !(variable.type().isLong() || variable.type().isInt())){
            this.createSignExtend(Register.RAX, primary);
        }
        return primary;
    }
    public void createIntegerDiv() {
        Register secondary = this.popTemporaryIntoSecondary();
        this.popTemporaryIntoPrimary();
        this.addInstruction(OperationType.XOR, new Address<>(Register.RDX), new Address<>(Register.RDX));
        this.addInstruction(OperationType.IDIV, new Address<>(secondary), null);
    }

    public void createIntegerMul() {
        this.popTemporaryIntoPrimary();
        Register secondary = this.popTemporaryIntoSecondary();
        this.addInstruction(OperationType.MUL, new Address<>(secondary), null);
    }
    private void createBinary() throws CompileException {
        Quad current = this.getCurrentQuad();
        if(current.op() == QuadOp.MUL && current.result().type.isInteger()){
            createIntegerMul();
        } else if(current.op() == QuadOp.DIV && current.result().type.isInteger()){
            createIntegerDiv();
        }else{
            OperationType op = OperationType.getOpFromResultType(current.op(), current.result().type);
            Register secondary   = this.popTemporaryIntoSecondary();
            Register primary     = this.popTemporaryIntoPrimary();
            this.addInstruction(op, new Address<>(primary), new Address<>(secondary));
        }
        this.addTemporary(current.result().type);
    }
    private void createLabel() throws CompileException {
        Quad currentQuad = this.getCurrentQuad();
        this.instructions.add(new IntelInstruction(new Label(currentQuad.operand1().name)));
    }
    private void createJmp() throws CompileException {
        Quad currentQuad = this.getCurrentQuad();
        this.instructions.add(new IntelInstruction(new Operation(OperationType.JMP), new Address<>(currentQuad.operand1().name)));
    }
    private void createCast() {}
    private void createShift() throws CompileException {
        Quad current = this.getCurrentQuad();
        OperationType op = OperationType.getBinaryOpFromQuadOp(current.op());
        this.popTemporaryIntoSecondary();
        Register primary     = this.popTemporaryIntoPrimary();
        this.addInstruction(op, new Address<>(primary), new Address<>(Register.CL));
        this.addTemporary(current.result().type);
    }
    private void createMod() throws CompileException {
        DataType type = this.getCurrentQuad().result().type;
        this.addInstruction(OperationType.XOR, new Address<>(Register.RDX), new Address<>(Register.RDX));
        this.popIntoRegister(Register.SECONDARY_GENERAL_REGISTER);
        this.popTemporaryIntoPrimary();

        this.addInstruction(OperationType.IDIV, new Address<>(Register.SECONDARY_GENERAL_REGISTER), null);
        this.addInstruction(
                Operation.getMoveOpFromType(type),
                new Address<>(Register.getPrimaryRegisterFromDataType(type)),
                new Address<>(Register.getThirdRegisterFromDataType(type))
        );
        this.addTemporary(type);
    }
    private void createIncFloat(OperationType op, Symbol result, boolean post) throws CompileException {
        this.setupPostfixFloat(result, result.type);
        if(post){
            this.addTemporary(result.type);
            this.immediateArithmeticFloat(op.convertToFloatingPoint(result.type), result);
            return;
        }
        this.immediateArithmeticFloat(op.convertToFloatingPoint(result.type), result);
        this.addTemporary(result.type);
    }
    private void createInc() throws CompileException {
        Quad current = this.getCurrentQuad();
        Symbol result =current.result();
        OperationType op    = OperationType.getBinaryOpFromQuadOp(current.op());
        boolean post = current.op() == QuadOp.POST_INC;
        if(result.type.isFloatingPoint()){
            this.createIncFloat(op, result, post);
        }else{
            OperationType pointerOp = op == OperationType.INC ? OperationType.ADD : OperationType.SUB;
            this.createPostfixInteger(op, pointerOp, post, result, current.operand1().type);
        }
    }
    public void immediateArithmeticFloat(OperationType arithmeticOp, Symbol result){
        OperationType moveOp = Operation.getMoveOpFromType(result.type);
        this.addInstruction(arithmeticOp, new Address<>(Register.PRIMARY_SSE_REGISTER), new Address<>(Register.SECONDARY_SSE_REGISTER));
        this.addInstruction(moveOp, new Address<>(Register.RAX, true), new Address<>(Register.PRIMARY_SSE_REGISTER));
    }
    public void createPostfixInteger(OperationType op, OperationType pointerOp, boolean post, Symbol result, DataType target) throws CompileException {
        this.popTemporaryIntoSecondary();
        Address<?> primary = new Address<>(Register.getPrimaryRegisterFromDataType(target));
        this.addInstruction(OperationType.MOV, primary, new Address<>(Register.RCX, true));
        if(post){
            this.addTemporary(result.type);
        }
        if(target.isPointer()){
            int pointerSize = target.getTypeFromPointer().getSize();
            this.addInstruction(pointerOp, primary, new Address<>(pointerSize));
        }else{
            this.addInstruction(op, primary, null);
        }
        this.addInstruction(OperationType.MOV, new Address<>(Register.RCX, true), primary);

        if(!post){
            this.addTemporary(result.type);
        }
    }
    public void setupPostfixFloat(Symbol result, DataType target) {
        this.popTemporaryIntoPrimary();
        this.addInstruction(Operation.getMoveOpFromType(result.type), new Address<>(Register.getPrimaryRegisterFromDataType(target)), new Address<>(Register.RAX, true));

        this.addInstruction(OperationType.MOV, new Address<>(Register.SECONDARY_GENERAL_REGISTER), new Address<>(1));
        OperationType convertOp = result.type.isDouble() ? OperationType.CVTSI2SD : OperationType.CVTSI2SS;
        this.addInstruction(convertOp, new Address<>(Register.SECONDARY_SSE_REGISTER), new Address<>(Register.SECONDARY_GENERAL_REGISTER));
    }
    private void createNegate() throws CompileException {
        Symbol result       = getCurrentQuad().result();
        Register primary    = this.popTemporaryIntoPrimary();
        this.addInstruction(OperationType.NOT, new Address<>(primary), null);
        this.addInstruction(OperationType.INC, new Address<>(primary), null);
        this.addTemporary(result.type);
    }

    public void createCompare(OperationType compareOp, OperationType setOp, Register left, Address<?> right){
        this.addInstruction(compareOp, new Address<>(left), new Address<>(right));
        this.addInstruction(setOp, new Address<>(Register.AL), null);
    }
    private void createLogical() throws CompileException {
        Register right = popTemporaryIntoSecondary();
        Register left = popTemporaryIntoPrimary();
        Quad current = this.getCurrentQuad();

        int firstRegister = current.op() == QuadOp.LOGICAL_OR ? 1 : 0;

        this.addInstruction(OperationType.CMP, new Address<>(left), new Address<>(firstRegister));
        String mergeLabel        = symbolTable.generateLabel().name;

        this.addInstruction(OperationType.JE, new Address<>(mergeLabel), null);
        createCompare(OperationType.CMP, OperationType.SETE, right, new Address<>(1));
        this.instructions.add(new IntelInstruction(new Label(mergeLabel), null, null));
        addTemporary(current.result().type);
    }
    private void createComparison() throws CompileException {
        Register right = popTemporaryIntoSecondary();
        Register left = popTemporaryIntoPrimary();
        Quad current = this.getCurrentQuad();
        OperationType op = OperationType.getOpFromResultType(current.op(), current.result().type);
        if(current.operand1().type.isFloatingPoint()){
            op = op.convertToFloat();
        }

        OperationType cmpOp = OperationType.getCmpOpFromType(current.operand1().type);
        createCompare(cmpOp, op, left, new Address<>(right));
        this.addTemporary(current.result().type);
    }
    private void createNot() throws CompileException {
        Quad current = this.getCurrentQuad();
        if(current.result().type.isFloatingPoint()){
            Register primary = this.popTemporaryIntoPrimary();
            this.addInstruction(OperationType.MOV, new Address<>(Register.RAX), new Address<>(0));
            OperationType op = OperationType.getCmpOpFromType(current.result().type);
            OperationType moveOp = OperationType.getConvertOpFromType(DataType.getInt(), current.result().type);
            this.addInstruction(moveOp, new Address<>(Register.SECONDARY_SSE_REGISTER), new Address<>(Register.RAX));
            this.createCompare(op, OperationType.SETE, primary, new Address<>(Register.SECONDARY_SSE_REGISTER));
            this.addTemporary(current.result().type);
        }else{
            Register primary = this.popTemporaryIntoPrimary();
            this.createCompare(OperationType.CMP, OperationType.SETE, primary, new Address<>(0));
            this.addTemporary(current.result().type);
        }
    }
    public void deallocateStackSpace(int space){this.addInstruction(OperationType.ADD, new Address<>(Register.RSP), new Address<>(space));}
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
            this.addTemporary(current.result().type);
        }
    }
    private void createLoadImmediate() throws CompileException {
        Quad current = this.getCurrentQuad();
        Symbol result = current.result();
        ImmediateSymbol immediateSymbol = (ImmediateSymbol) current.operand1();
        if(current.result().type.isString() || current.result().type.isFloatingPoint()) {
            Constant constant = symbolTable.getConstants().get(immediateSymbol.getValue());
            this.addInstruction(OperationType.getMoveOpFromType(result.type), new Address<>(Register.getPrimaryRegisterFromDataType(result.type)), new Address<>(constant.label(), !result.type.isString()));
        }else{
            this.addInstruction(OperationType.getMoveOpFromType(result.type), new Address<>(Register.getPrimaryRegisterFromDataType(result.type)), new Address<>(immediateSymbol.getValue()));
        }
        this.addTemporary(result.type);    }
    private void createLoad() throws CompileException {
        Quad current = getCurrentQuad();
        VariableSymbol variable = (VariableSymbol) current.operand1();
        Symbol result = current.result();
        OperationType op = (variable.type.isStruct() || current.op() == QuadOp.LOAD_POINTER) ? OperationType.LEA : OperationType.getMoveOpFromType(variable.type);
        this.addInstruction(op, new Address<>(Register.getPrimaryRegisterFromDataType(result.type)), new Address<>(Register.RBP, true, variable.offset));
        this.addTemporary(result.type);
    }
    private void createStoreArrayItem() throws CompileException {
        Quad current = getCurrentQuad();
        ArrayItemSymbol arraySymbol = (ArrayItemSymbol)current.operand1();
        ArrayDataType arrayDataType = (ArrayDataType)arraySymbol.type;
        int offset = arraySymbol.offset + arraySymbol.getOffset();
        if(arrayDataType.itemType.isStruct()){
            this.popIntoRegister(Register.RSI);
            this.addInstruction(OperationType.LEA, new Address<>(Register.RDI), new Address<>(Register.RBP, true, offset));
            this.createMovSB(arrayDataType.itemType);
            return;
        }
        this.popTemporaryIntoPrimary();
        OperationType moveOp = OperationType.getMoveOpFromType(arrayDataType.itemType);
        this.addInstruction(moveOp, new Address<>(Register.RBP, true, offset), new Address<>(Register.getPrimaryRegisterFromDataType(arrayDataType.itemType)));
    }
    private void addEpilogue(){
        instructions.add(new IntelInstruction(new Operation(OperationType.MOV), new Address<>(Register.RSP), new Address<>(Register.RBP)));
        instructions.add(new IntelInstruction(new Operation(OperationType.POP), new Address<>(Register.RBP)));
    }
    private void createIMul() throws CompileException {
        Quad current = this.getCurrentQuad();
        ImmediateSymbol immediateSymbol = (ImmediateSymbol) current.operand2();
        Register primary = this.popTemporaryIntoPrimary();
        this.addInstruction(OperationType.IMUL, new Address<>(primary), new Address<>(immediateSymbol.getValue()));
        this.addTemporary(current.result().type);
    }
    public void assignStruct(DataType structType){
        popIntoRegister(Register.RDI);
        popIntoRegister(Register.RSI);
        this.createMovSB(structType);
    }
    public void createRepeatMoveSingleByte(){this.addInstruction(OperationType.REP, new Address<>(Register.MOVSB), null);}
    public void createMovSB(DataType structType){
        int immediate = SymbolTable.getStructSize(symbolTable.getStructs(), structType);
        this.addInstruction(OperationType.MOV, new Address<>(Register.RCX), new Address<>(immediate));
        this.createRepeatMoveSingleByte();
    }
    private void createAssign() throws CompileException {
        Quad current = this.getCurrentQuad();
        DataType targetType = current.operand1().type;
        if(targetType.isStruct()){
            this.assignStruct(targetType);
        }else{
            TemporaryStackVariable pointer  = this.temporaryStack.popVariable();
            TemporaryStackVariable value    = this.temporaryStack.popVariable();
            this.addInstruction(OperationType.getMoveOpFromType(value.type()), new Address<>(Register.getPrimaryRegisterFromDataType(value.type())), new Address<>(Register.RBP, true, value.offset()));
            this.addInstruction(OperationType.getMoveOpFromType(pointer.type()), new Address<>(Register.getSecondaryRegisterFromDataType(pointer.type())), new Address<>(Register.RBP, true, pointer.offset()));
            this.addInstruction(OperationType.getMoveOpFromType(targetType), new Address<>(Register.RCX, true), new Address<>(Register.getPrimaryRegisterFromDataType(targetType)));
        }
    }
    private void createPointerArithmetic(DataType pointerType) throws CompileException {
        int size = symbolTable.getStructSize(pointerType.getTypeFromPointer());
        this.addInstruction(OperationType.IMUL, new Address<>(Register.PRIMARY_GENERAL_REGISTER), new Address<>(size));
        this.addInstruction(OperationType.ADD, new Address<>(Register.PRIMARY_GENERAL_REGISTER), new Address<>(Register.SECONDARY_GENERAL_REGISTER));
    }
    private void createAllocate() throws CompileException {
        Symbol immediateSymbol =  getCurrentQuad().operand1();
        this.addInstruction(OperationType.SUB, new Address<>(Register.RSP), new Address<>(immediateSymbol.name));
    }
    private void createJumpOnCondition() throws CompileException {
        Symbol dest = getCurrentQuad().operand1();
        Address<?> immediate = new Address<>(getCurrentQuad().op() == QuadOp.JMP_T ? 1 : 0); popTemporaryIntoPrimary();
        OperationType cmpOp = OperationType.getCmpOpFromType(dest.type);
        this.addInstruction(cmpOp, new Address<>(Register.PRIMARY_GENERAL_REGISTER), immediate);
        this.addInstruction(OperationType.JE, new Address<>(dest.name), null);
    }
    private void createDereference() throws CompileException {
        Symbol result = getCurrentQuad().result();
        Register primary = this.popTemporaryIntoPrimary();
        this.addInstruction(OperationType.getMoveOpFromType(result.type), new Address<>(Register.getPrimaryRegisterFromDataType(result.type)), new Address<>(primary, true));
        this.addTemporary(result.type);
    }
    private void createIndex() throws CompileException {
        Quad current = getCurrentQuad();
        Symbol result = current.result();

        popTemporaryIntoPrimary();
        popTemporaryIntoSecondary();
        this.createPointerArithmetic(current.operand1().type);

        if(current.op() == QuadOp.INDEX){
            OperationType operationType = result.type.isStruct() ? OperationType.LEA : OperationType.getMoveOpFromType(result.type);
            this.addInstruction(operationType, new Address<>(Register.getPrimaryRegisterFromDataType(result.type)), new Address<>(Register.PRIMARY_GENERAL_REGISTER, true));
        }
        this.addTemporary(result.type);
    }
    private void createLoadMember() throws CompileException {
        Quad current = this.getCurrentQuad();
        Symbol result = current.result();
        MemberSymbol member = (MemberSymbol) current.operand1();
        QuadOp op = current.op();

        this.popTemporaryIntoPrimary();
        OperationType operationType = (result.type.isStruct() || op == QuadOp.LOAD_MEMBER_POINTER) ? OperationType.LEA : OperationType.getMoveOpFromType(result.type);
        this.addInstruction(operationType, new Address<>(Register.getPrimaryRegisterFromDataType(result.type)), new Address<>(Register.RAX, true, member.getOffset()));
        this.addTemporary(result.type);
    }
    public void addExternalParameter(ArgumentSymbol argumentSymbol) {
        TemporaryStackVariable param = this.temporaryStack.peekVariable();
        popTemporaryIntoPrimary();
        Register[] registers = argumentSymbol.type.isFloatingPoint() ? LINUX_FLOAT_PARAM_LOCATIONS : LINUX_GENERAL_PARAM_LOCATIONS;
        if(argumentSymbol.getCount() >= registers.length){
            this.addInstruction(OperationType.getMoveOpFromType(argumentSymbol.type), new Address<>(Register.RSP, true, argumentSymbol.getOffset()), new Address<>(Register.getPrimaryRegisterFromDataType(argumentSymbol.type)));
        }else{
            Register source = param.type().isFloatingPoint() ? Register.PRIMARY_SSE_REGISTER : Register.PRIMARY_GENERAL_REGISTER;
            this.addInstruction(OperationType.getMoveOpFromType(param.type()), new Address<>(registers[argumentSymbol.getCount()]), new Address<>(source));
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
        }else {
            if(!argumentSymbol.getExternal()){
                this.popTemporaryIntoPrimary();
                this.addInstruction(OperationType.getMoveOpFromType(argumentSymbol.type), new Address<>(Register.RSP, true, argumentSymbol.getOffset()), new Address<>(Register.getPrimaryRegisterFromDataType(argumentSymbol.type)));
            }else{
                this.addExternalParameter(argumentSymbol);
            }
        }
    }
    private void createConvert() throws CompileException {
        Quad current = getCurrentQuad();
        Symbol dest = current.operand1();
        Symbol result = current.result();
        if(dest.type.isInteger() && result.type.isInteger() && dest.type.isSameType(DataType.getHighestDataTypePrecedence(dest.type, result.type))){
            return;
        }
        popTemporaryIntoPrimary();

        OperationType convert = OperationType.getConvertOpFromType(dest.type, result.type);
        Register target = Register.getMinimumConvertTarget(convert, result.type);
        Register source = Register.getMinimumConvertSource(convert, dest.type);
        this.addInstruction(convert, new Address<>(target), new Address<>(source));
        addTemporary(result.type);
    }
    private void createReturn() throws CompileException {
        if(this.getCurrentQuad().result() != null){
            this.popTemporaryIntoPrimary();
        }
        this.addEpilogue();
        this.instructions.add(new IntelInstruction(new Operation(OperationType.RET)));
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
