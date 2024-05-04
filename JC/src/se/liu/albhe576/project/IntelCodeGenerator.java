package se.liu.albhe576.project;

import java.util.*;

import static se.liu.albhe576.project.Register.*;
import static se.liu.albhe576.project.OperationType.*;

public class IntelCodeGenerator implements CodeGenerator{

    @FunctionalInterface private interface Generate{ void generate(Quad quad) throws CompileException;}
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
    public static final Register[] LINUX_FLOAT_PARAM_LOCATIONS      = new Register[]{XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7};
    public static final Register[] LINUX_GENERAL_PARAM_LOCATIONS    = new Register[]{RDI, RSI, RDX, RCX, R8, R9};
    private final SymbolTable symbolTable;
    private List<Instruction> instructions;
    private TemporaryVariableStack temporaryStack;
    public Map<String, List<Instruction>> generateInstructions(Map<String, QuadList> functionQuads) throws CompileException {
        final Map<String, List<Instruction>> functions = new HashMap<>();

        for(Map.Entry<String, QuadList> function : functionQuads.entrySet()){
            this.instructions       = new ArrayList<>();
            this.temporaryStack     = new TemporaryVariableStack(symbolTable, function.getKey());

            this.addPrologue();
            final int allocateStackSpaceIndex = this.instructions.size();
            for(Quad quad : function.getValue()){
                this.INSTRUCTION_GENERATION_MAP.get(quad.op()).generate(quad);
            }
            if(!this.instructions.get(this.instructions.size() - 1).isReturn()){
                this.addEpilogue();
                this.instructions.add(new IntelInstruction(new Operation(RET)));
            }

            int stackSpace  = -1 * temporaryStack.getMaxOffset();
            stackSpace += Compiler.getStackAlignment(stackSpace);
            if(stackSpace != 0){
                this.instructions.add(allocateStackSpaceIndex, new IntelInstruction(new Operation(SUB), new Address<>(RSP), new Immediate<>(stackSpace)));
            }
            functions.put(function.getKey(), this.instructions);
        }

        return functions;
    }
    private void addInstruction(OperationType op, Register dest){
        this.instructions.add(new IntelInstruction(new Operation(op), new Address<>(dest), null));
    }
    private void addInstruction(OperationType op, Operand dest){
        this.instructions.add(new IntelInstruction(new Operation(op), dest, null));
    }
    private void addInstruction(OperationType op, Operand dest, Register source){
        this.instructions.add(new IntelInstruction(new Operation(op), dest, new Address<>(source)));
    }
    private void addInstruction(OperationType op, Register destRegister, Operand source){
        Address<?> dest     = destRegister == null ? null : new Address<>(destRegister);
        this.instructions.add(new IntelInstruction(new Operation(op), dest, source));
    }
    private void addInstruction(OperationType op, Register dest, Register source){
        this.instructions.add(new IntelInstruction(new Operation(op), new Address<>(dest), new Address<>(source)));
    }
    private void addInstruction(OperationType op, Operand dest, Operand source){
        this.instructions.add(new IntelInstruction(new Operation(op), dest, source));
    }
    private void addTemporary(DataType type) throws CompileException {
        type                    = type.isStruct() ? type.getPointerFromType() : type;
        int offset              = this.temporaryStack.push(type);
        OperationType  moveOp        = Operation.getMoveOpFromType(type);
        this.addInstruction(moveOp, this.getEffectiveAddress(RBP, offset), new Address<>(getPrimaryRegisterFromDataType(type)));
    }
    private Register popTemporaryIntoSecondary() throws CompileException {
        TemporaryVariable value    = this.temporaryStack.peek();
        Register secondary = getSecondaryRegisterFromDataType(value.type());
        popIntoRegister(secondary);
        if(value.type().isInteger() && !(value.type().isLong() || value.type().isInt())){
            this.createSignExtend(SECONDARY_GENERAL_REGISTER, new Address<>(secondary));
        }
        return secondary;
    }
    private OperationType popIntoRegister(Register primary){
        TemporaryVariable variable = this.temporaryStack.pop();
        OperationType moveOp                = Operation.getMoveOpFromType(variable.type());
        this.addInstruction(moveOp, primary, this.getEffectiveAddress(RBP, variable.offset()));
        return moveOp;
    }
    private void createSignExtend(Register dest, Operand source){
        this.addInstruction(MOVSX, dest, source);
    }
    private Register popTemporaryIntoPrimary() throws CompileException {
        TemporaryVariable variable = this.temporaryStack.peek();
        Register primary       = getPrimaryRegisterFromDataType(variable.type());
        OperationType lastOp            = this.popIntoRegister(primary);
        if(lastOp != MOVSX && variable.type().isInteger() && !(variable.type().isLong() || variable.type().isInt())){
            this.createSignExtend(RAX, new Address<>(primary));
        }
        return primary;
    }
    private void createBinary(Quad quad) throws CompileException {
        DataType result        = quad.result().type;
        Register secondary     = this.popTemporaryIntoSecondary();
        Register primary       = this.popTemporaryIntoPrimary();
        OperationType  op      = getOpFromResultType(quad.op(), result);

        if(quad.op()== QuadOp.DIV && result.isInteger()){
            this.createIntegerDivision();
        } else if(quad.op() == QuadOp.MUL && result.isInteger()){
            this.addInstruction(op, new Address<>(secondary));
        } else{
            this.addInstruction(op, primary, secondary);
        }
        this.addTemporary(result);
    }
    private void createLabel(Quad quad) {
        this.instructions.add(new IntelInstruction(new Label(quad.operand1().name)));
    }
    private void createJmp(Quad quad) {
        this.instructions.add(new IntelInstruction(new Operation(JMP), new Immediate<>(quad.operand1().name)));
    }
    private void createCast(Quad quad) {}
    private void createShift(Quad quad) throws CompileException {
        OperationType   op   = getBinaryOpFromQuadOp(quad.op());
        this.popTemporaryIntoSecondary();
        Register primary     = this.popTemporaryIntoPrimary();
        this.addInstruction(op, primary, CL);
        this.addTemporary(quad.result().type);
    }
    private void createIntegerDivision(){
        this.addInstruction(XOR, RDX, RDX);
        this.addInstruction(IDIV, SECONDARY_GENERAL_REGISTER);
    }
    private void createMod(Quad quad) throws CompileException {
        DataType type = quad.result().type;
        this.popTemporaryIntoSecondary();
        this.popTemporaryIntoPrimary();
        this.createIntegerDivision();
        this.createMoveIntoPrimary(type, new Address<>(getThirdRegisterFromDataType(type)));
        this.addTemporary(type);
    }
    private void createPostfixFloat(OperationType op, DataType result, boolean post) throws CompileException {
        this.setupPostfixFloat(result);
        op = op.convertToFloatingPoint(result);
        if(post){
            this.addTemporary(result);
            this.immediateArithmeticFloat(op, result);
        }else{
            this.immediateArithmeticFloat(op, result);
            this.addTemporary(result);
        }
    }
    private void createInc(Quad quad) throws CompileException {
        DataType result = quad.result().type;
        OperationType op    = getBinaryOpFromQuadOp(quad.op());
        boolean post = quad.op() == QuadOp.POST_INC;
        if(result.isFloatingPoint()){
            this.createPostfixFloat(op, result, post);
        }else{
            OperationType pointerOp = op == INC ? ADD : SUB;
            this.createPostfixInt(op, pointerOp, post, result);
        }
    }

    private void immediateArithmeticFloat(OperationType arithmeticOp, DataType result){
        OperationType moveOp = Operation.getMoveOpFromType(result);
        this.addInstruction(arithmeticOp, PRIMARY_SSE_REGISTER, SECONDARY_SSE_REGISTER);
        this.addInstruction(moveOp, this.getEffectiveAddress(RAX), PRIMARY_SSE_REGISTER);
    }
    private void createPostfixInt(OperationType op, OperationType pointerOp, boolean post, DataType target) throws CompileException {
        this.popTemporaryIntoSecondary();
        this.createMoveIntoPrimary(target, this.getEffectiveAddress(RCX));

        if(post){
            this.addTemporary(target);
        }

        Address<?> primary = new Address<>(getPrimaryRegisterFromDataType(target));
        if(target.isPointer()){
            int pointerSize = target.getTypeFromPointer().getSize();
            this.addInstruction(pointerOp, primary, new Immediate<>(pointerSize));
        }else{
            this.addInstruction(op, primary);
        }
        this.createMove(DataType.getInt(), this.getEffectiveAddress(RCX), primary);

        if(!post){
            this.addTemporary(target);
        }
    }
    private void setupPostfixFloat(DataType result) throws CompileException {
        this.popTemporaryIntoPrimary();
        this.createMoveIntoPrimary(result, this.getEffectiveAddress(RAX));
        this.createMove(DataType.getInt(), new Address<>(SECONDARY_GENERAL_REGISTER), new Immediate<>(1));

        OperationType convertOp = result.isDouble() ? CVTSI2SD : CVTSI2SS;
        this.addInstruction(convertOp, SECONDARY_SSE_REGISTER, SECONDARY_GENERAL_REGISTER);
    }
    private void createNegate(Quad quad) throws CompileException {
        Register primary    = this.popTemporaryIntoPrimary();
        this.addInstruction(NOT, primary);
        this.addInstruction(INC, primary);
        this.addTemporary(quad.result().type);
    }

    private void createCompare(OperationType compareOp, OperationType setOp, Register left, Operand right){
        this.addInstruction(compareOp, left, right);
        this.addInstruction(setOp, new Address<>(AL));
    }
    private void createLabel(String label){
        this.instructions.add(new IntelInstruction(new Label(label), null, null));
    }
    private void createLogical(Quad quad) throws CompileException {
        Register right  = popTemporaryIntoSecondary();
        Register left   = popTemporaryIntoPrimary();

        // Rename
        int firstRegister = quad.op() == QuadOp.LOGICAL_OR ? 1 : 0;

        this.addInstruction(CMP, left, new Immediate<>(firstRegister));
        String mergeLabel        = symbolTable.generateLabel().name;

        this.addInstruction(JE, new Immediate<>(mergeLabel));
        this.createCompare(CMP, SETE, right, new Immediate<>(1));
        this.createLabel(mergeLabel);
        this.addTemporary(quad.result().type);
    }
    private void createComparison(Quad quad) throws CompileException {
        Register right          = popTemporaryIntoSecondary();
        Register left           = popTemporaryIntoPrimary();
        OperationType op        = getOpFromResultType(quad.op(), quad.result().type);
        DataType destination    = quad.operand1().type;

        if(destination.isFloatingPoint()){
            op = op.convertToFloat();
        }

        OperationType cmpOp = getCmpOpFromType(destination);
        createCompare(cmpOp, op, left, new Address<>(right));
        this.addTemporary(quad.result().type);
    }
    private void createNotFloat(DataType resultType) throws CompileException {
        Register primary        = this.popTemporaryIntoPrimary();
        this.createMoveIntoPrimary(DataType.getInt(), new Immediate<>(0));

        OperationType op        = getCmpOpFromType(resultType);
        OperationType convertOp = getConvertOpFromType(DataType.getInt(), resultType);

        this.addInstruction(convertOp, SECONDARY_SSE_REGISTER, RAX);
        this.createCompare(op, SETE, primary, new Address<>(SECONDARY_SSE_REGISTER));
    }
    private void createNot(Quad quad) throws CompileException {
        if(quad.result().type.isFloatingPoint()){
            this.createNotFloat(quad.result().type);
        }else{
            Register primary = this.popTemporaryIntoPrimary();
            this.createCompare(CMP, SETE, primary, new Immediate<>(0));
        }
        this.addTemporary(quad.result().type);
    }
    private void deallocateStackSpace(int space){
        this.addInstruction(ADD, RSP, new Immediate<>(space));
    }
    private void createCall(Quad quad) throws CompileException {
        ImmediateSymbol stackSizeImmediate = (ImmediateSymbol) quad.operand2();
        this.addInstruction(CALL, new Immediate<>(quad.operand1().name));

        int stackSize    = Integer.parseInt(stackSizeImmediate.getValue());
        stackSize       += Compiler.getStackAlignment(stackSize);
        if(stackSize != 0){
            this.deallocateStackSpace(stackSize);
        }

        if(!quad.result().type.isVoid()){
            this.addTemporary(quad.result().type);
        }
    }
    private Address<?> getImmediate(ImmediateSymbol immediateSymbol, DataType result){
        if(result.isString() || result.isFloatingPoint()) {
            Constant constant = symbolTable.getConstants().get(immediateSymbol.getValue());
            return new Address<>(constant.label(), !result.isString());
        }
        return new Address<>(immediateSymbol.getValue());
    }
    private void createMove(DataType destinationType, Address<?> destination, Operand source){
        this.addInstruction(getMoveOpFromType(destinationType), destination, source);
    }
    private void createMoveIntoPrimary(DataType destinationType, Operand source) throws CompileException {
        this.createMove(destinationType, new Address<>(getPrimaryRegisterFromDataType(destinationType)), source);
    }
    private void createLoadImmediate(Quad quad) throws CompileException {
        DataType result                     = quad.result().type;
        Address<?> immediate                = getImmediate((ImmediateSymbol) quad.operand1(), result);

        this.createMoveIntoPrimary(result, immediate);
        this.addTemporary(result);
    }
    private void createLoad(Quad quad) throws CompileException {
        VariableSymbol variable = (VariableSymbol) quad.operand1();
        DataType result         = quad.result().type;

        Address<?> source = this.getEffectiveAddress(RBP, variable.offset);
        if(variable.type.isStruct() || quad.op() == QuadOp.LOAD_POINTER){
            this.createLoadEffectiveAddress(getPrimaryRegisterFromDataType(result), source);
        }else{
            this.createMoveIntoPrimary(variable.type, source);
        }
        this.addTemporary(result);
    }
    private Address<?> getEffectiveAddress(Register register){
        return new Address<>(register, true, 0);
    }
    private Address<?> getEffectiveAddress(Register register, int offset){
        return new Address<>(register, true, offset);
    }
    private void createLoadEffectiveAddress(Register destination, Address<?> source){
        this.addInstruction(LEA, destination, source);
    }
    private void createStoreArrayItem(Quad quad) throws CompileException {
        ArrayItemSymbol arraySymbol = (ArrayItemSymbol) quad.operand1();
        ArrayDataType arrayDataType = (ArrayDataType)   arraySymbol.type;

        Address<?> stackAddress = this.getEffectiveAddress(RBP, arraySymbol.offset + arraySymbol.getOffset());
        if(arrayDataType.itemType.isStruct()){
            this.popIntoRegister(RSI);
            this.createLoadEffectiveAddress(RDI, stackAddress);
            this.createMovSB(arrayDataType.itemType);
        }else{
            this.popTemporaryIntoPrimary();
            OperationType moveOp = getMoveOpFromType(arrayDataType.itemType);
            this.addInstruction(moveOp, stackAddress, getPrimaryRegisterFromDataType(arrayDataType.itemType));
        }

    }
    private void addPrologue(){
        this.addInstruction(PUSH, RBP);
        this.addInstruction(MOV, RBP, RSP);
    }
    private void addEpilogue(){
        this.addInstruction(MOV, RSP, RBP);
        this.addInstruction(POP, RBP);
    }
    private void createIMul(Quad quad) throws CompileException {
        ImmediateSymbol immediateSymbol = (ImmediateSymbol) quad.operand2();
        Register primary                = this.popTemporaryIntoPrimary();
        this.addInstruction(IMUL, primary, new Immediate<>(immediateSymbol.getValue()));
        this.addTemporary(quad.result().type);
    }
    private void createMovSB(DataType structType){
        int immediate = SymbolTable.getStructSize(symbolTable.getStructs(), structType);
        this.addInstruction(MOV, RCX, new Immediate<>(immediate));
        this.addInstruction(REP, new Operation(MOVSB));
    }
    private void createAssign(Quad quad) throws CompileException {
        DataType targetType = quad.operand1().type;
        if(targetType.isStruct()){
            popIntoRegister(RDI);
            popIntoRegister(RSI);
            this.createMovSB(targetType);
        }else{
            TemporaryVariable pointer  = this.temporaryStack.pop();
            TemporaryVariable value    = this.temporaryStack.pop();
            this.createMoveIntoPrimary(value.type(), this.getEffectiveAddress(RBP, value.offset()));
            this.createMove(pointer.type(), new Address<>(getSecondaryRegisterFromDataType(pointer.type())), this.getEffectiveAddress(RBP, pointer.offset()));
            this.createMove(targetType, this.getEffectiveAddress(RCX), new Address<>(getPrimaryRegisterFromDataType(targetType)));
        }
    }
    private void createAllocate(Quad quad) {
        Symbol immediateSymbol =  quad.operand1();
        this.addInstruction(SUB, RSP, new Immediate<>(immediateSymbol.name));
    }
    private void createJumpOnCondition(Quad quad) throws CompileException {
        Symbol dest             = quad.operand1();
        int jumpOnTrue          = quad.op() == QuadOp.JMP_T ? 1 : 0;
        popTemporaryIntoPrimary();

        OperationType cmpOp = getCmpOpFromType(dest.type);
        this.addInstruction(cmpOp, PRIMARY_GENERAL_REGISTER, new Immediate<>(jumpOnTrue));
        this.addInstruction(JE, new Immediate<>(dest.name));
    }
    private void createDereference(Quad quad) throws CompileException {
        DataType result     = quad.result().type;
        Register primary    = this.popTemporaryIntoPrimary();
        this.createMoveIntoPrimary(result, this.getEffectiveAddress(primary));
        this.addTemporary(result);
    }
    private void createIndex(Quad quad) throws CompileException {
        DataType target = quad.result().type;

        popTemporaryIntoPrimary();
        popTemporaryIntoSecondary();
        int size = symbolTable.getStructSize(quad.operand1().type.getTypeFromPointer());
        this.addInstruction(IMUL, PRIMARY_GENERAL_REGISTER, new Immediate<>(size));
        this.addInstruction(ADD, PRIMARY_GENERAL_REGISTER, SECONDARY_GENERAL_REGISTER);

        if(quad.op() == QuadOp.INDEX){
            Register destination = getPrimaryRegisterFromDataType(target);
            if(target.isStruct()){
                this.createLoadEffectiveAddress(destination, this.getEffectiveAddress(PRIMARY_GENERAL_REGISTER));
            }else{
                this.createMove(target, new Address<>(destination), this.getEffectiveAddress(PRIMARY_GENERAL_REGISTER));
            }
        }

        this.addTemporary(target);
    }
    private void createLoadMember(Quad quad) throws CompileException {
        DataType result             = quad.result().type;
        MemberSymbol member         = (MemberSymbol) quad.operand1();
        Address<?> memberAddress    = this.getEffectiveAddress(PRIMARY_GENERAL_REGISTER, member.getOffset());

        this.popTemporaryIntoPrimary();
        if(result.isStruct() || quad.op() == QuadOp.LOAD_MEMBER_POINTER){
            this.createLoadEffectiveAddress(PRIMARY_GENERAL_REGISTER, memberAddress);
        }else{
            this.createMoveIntoPrimary(result, memberAddress);
        }
        this.addTemporary(result);
    }
    private void addExternalParameter(ArgumentSymbol argumentSymbol) throws CompileException {
        TemporaryVariable param    = this.temporaryStack.peek();
        Register[] registers = argumentSymbol.type.isFloatingPoint() ? LINUX_FLOAT_PARAM_LOCATIONS : LINUX_GENERAL_PARAM_LOCATIONS;
        popTemporaryIntoPrimary();
        if(argumentSymbol.getCount() >= registers.length){
            this.addInstruction(getMoveOpFromType(argumentSymbol.type), this.getEffectiveAddress(RSP, argumentSymbol.getOffset()), getPrimaryRegisterFromDataType(argumentSymbol.type));
        }else{
            Register source = param.type().isFloatingPoint() ? PRIMARY_SSE_REGISTER : PRIMARY_GENERAL_REGISTER;
            this.createMove(param.type(), new Address<>(registers[argumentSymbol.getCount()]), new Address<>(source));
        }
    }
    private void moveStruct(ArgumentSymbol argument){
        popIntoRegister(RSI);
        this.createLoadEffectiveAddress(RDI, this.getEffectiveAddress(RSP, argument.getOffset()));
        this.createMovSB(argument.type);
    }
    private void createArgument(Quad quad) throws CompileException {
        ArgumentSymbol argumentSymbol = (ArgumentSymbol) quad.operand1();
        if(quad.operand1().type.isStruct()){
            this.moveStruct(argumentSymbol);
        }else if(!argumentSymbol.getExternal()){
            this.popTemporaryIntoPrimary();
            this.createMove(argumentSymbol.type, this.getEffectiveAddress(RSP, argumentSymbol.getOffset()), new Address<>(getPrimaryRegisterFromDataType(argumentSymbol.type)));
        }
        else{
            this.addExternalParameter(argumentSymbol);
        }
    }
    private void createConvert(Quad quad) throws CompileException {
        DataType dest       = quad.operand1().type;
        DataType result     = quad.result().type;
        if(dest.isInteger() && result.isInteger() && dest.isSameType(DataType.getHighestDataTypePrecedence(dest, result))){
            return;
        }
        popTemporaryIntoPrimary();

        OperationType convert   = getConvertOpFromType(dest, result);
        Register target         = getMinimumConvertTarget(convert, result);
        Register source         = getMinimumConvertSource(convert, dest);
        this.addInstruction(convert, target, source);
        this.addTemporary(result);
    }

    private void createReturn(Quad quad) throws CompileException {
        if(quad.result() != null){
            this.popTemporaryIntoPrimary();
        }
        this.addEpilogue();
        this.instructions.add(new IntelInstruction(new Operation(RET)));
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
