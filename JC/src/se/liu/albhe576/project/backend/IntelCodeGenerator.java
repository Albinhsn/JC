package se.liu.albhe576.project.backend;

import java.util.*;

import se.liu.albhe576.project.frontend.*;
import static se.liu.albhe576.project.backend.Register.*;
import static se.liu.albhe576.project.backend.Operation.*;


/**
 * Generator x86 assembly for the langauge for the given calling convention
 * Creates instructions and generates the output string for the language
 * <p>
 * We generate assembly primarily using one register only (two for binary expressions)
 * Which means we need to store temporary values somewhere (which in this case means the stack)
 * So every operation we emit is basically either a binary one taking two elements of the stack into a primary and secondary register, doing an operation and pushing it back
 * Doing the same with one element and just using the primary register or emitting an instruction such as RET that takes no argument
 * @param <T>, the calling convention the code generator should follow
 * @see CodeGenerator
 * @see CallingConvention
 * @see IntelInstruction
 */
public class IntelCodeGenerator<T extends CallingConvention> implements CodeGenerator{
    private final T callingConvention;
    private final SymbolTable symbolTable;
    private void generate(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        switch(intermediate.op()){
            case RET->
                this.createReturn(instructions, temporaryStack, intermediate);
            case LABEL->
                instructions.addLabel(intermediate.operand1().name);
            case JMP->
                instructions.createJump(intermediate.operand1().name);
            case LOAD_MEMBER_POINTER, LOAD_MEMBER ->
                this.createLoadMember(instructions, temporaryStack, intermediate);
            case LOAD_POINTER, LOAD ->
                this.createLoad(instructions, temporaryStack, intermediate);
            case CONVERT->
                this.createConvert(instructions, temporaryStack, intermediate);
            case INDEX, REFERENCE_INDEX->
                this.createIndex(instructions, temporaryStack, intermediate);
            case DEREFERENCE->
                this.createDereference(instructions, temporaryStack, intermediate);
            case JMP_T, JMP_F->
                this.createJumpOnCondition(instructions, temporaryStack, intermediate);
            case ASSIGN->
                this.createAssign(instructions, temporaryStack, intermediate);
            case IMUL->
                this.createImmediateMultiply(instructions, temporaryStack, intermediate);
            case ADD, SUB, MUL, DIV,AND, OR, XOR ->
                this.createBinary(instructions, temporaryStack, intermediate);
            case SHL, SHR->
                this.createShift(instructions, temporaryStack, intermediate);
            case MOD->
                this.createMod(instructions, temporaryStack, intermediate);
            case PRE_INC, PRE_DEC, POST_INC, POST_DEC->
                this.createPostfixAndPrefix(instructions, temporaryStack, intermediate);
            case NEGATE->
                this.createNegate(instructions, temporaryStack, intermediate);
            case LOGICAL_OR, LOGICAL_AND->
                this.createLogical(instructions, temporaryStack, intermediate);
            case LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL->
                this.createComparison(instructions, temporaryStack, intermediate);
            case NOT->
                this.createNot(instructions, temporaryStack, intermediate);
            case CALL->
                this.createCall(instructions, temporaryStack, intermediate);
            case LOAD_IMM->
                this.createLoadImmediate(instructions, temporaryStack, intermediate);
            case CAST->{
                // nop
            }
	    case STORE_ARRAY_ITEM->
                this.createStoreArrayItem(instructions, temporaryStack, intermediate);
            default -> {
                throw new CompileException(String.format("Can't execute op %s", intermediate.op()));
            }
        }
    }

    public Map<String, ? extends InstructionList> generateInstructions(SymbolTable symbolTable, Map<String, IntermediateList> functionIntermediates) throws CompileException {
        final Map<String, IntelInstructionList> functions = new HashMap<>();

        for(Map.Entry<String, IntermediateList> function : functionIntermediates.entrySet()){
            IntelInstructionList instructions = new IntelInstructionList();
            TemporaryVariableStack temporaryStack = new TemporaryVariableStack(symbolTable, function.getKey());

            // Create the prologue
            instructions.addPrologue();

            // We allocate the amount of space the function takes prior to executing it to avoid needing to push and pop and keep alignment if we call external functions
            // So we just take the index after the prologue and emit it afterwards
            // The amount of space we need is the maximum offset the temporaryStack was at + padding
            final int allocateStackSpaceIndex = instructions.size();
            for(Intermediate intermediate : function.getValue()){
                this.generate(instructions, temporaryStack, intermediate);
            }

            // Create the epilogue and emit return (might be redundant if the last one is already a return)
            instructions.addEpilogue();
            instructions.createReturn();

            // Handle stack alignment
            int stackSpace  = -1 * temporaryStack.getMaxOffset();
            stackSpace += Compiler.getStackAlignment(stackSpace);
            if(stackSpace != 0){
                instructions.addInstruction(allocateStackSpaceIndex, new IntelInstruction(SUB, new Address<>(RSP), new Immediate<>(stackSpace)));
            }

            functions.put(function.getKey(), instructions);
        }
        return functions;
    }
    private void createReturn(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        // Avoid to pop if the return is void
        if (intermediate.result() != null) {
            popTemporaryIntoPrimary(instructions, temporaryStack);
        }
        instructions.addEpilogue();
        instructions.createReturn();
    }
    public static void addTemporary(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, DataType type) throws CompileException {
        // Adds a temporary value to its corresponding place on the stack
        // Alignment is handled after the function is compiled so we just fit it into the top of the stack
        type                    = type.isStructure() ? type.getPointerFromType() : type;
        int offset              = temporaryStack.push(type);
        Operation moveOp        = Operation.getMoveOpFromType(type);
        instructions.addInstruction(moveOp, Address.getEffectiveAddress(RBP, offset), new Address<>(getPrimaryRegisterFromDataType(type)));
    }
    public static Register popTemporaryIntoSecondary(IntelInstructionList instructions, TemporaryVariableStack temporaryStack) throws CompileException {

        // Pops the temporary variable at the top of the stack into the secondary register
        TemporaryVariable value     = temporaryStack.peek();
        Register secondary          = getSecondaryRegisterFromDataType(value.type());
        return popTemporaryAndSignExtend(instructions, temporaryStack, value, secondary, SECONDARY_GENERAL_REGISTER);
    }
    public static Operation popIntoRegister(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Register primary){
        TemporaryVariable variable      = temporaryStack.pop();
        Operation moveOp                = Operation.getMoveOpFromType(variable.type());
        instructions.addInstruction(moveOp, primary, Address.getEffectiveAddress(RBP, variable.offset()));
        return moveOp;
    }
    public static Register popTemporaryIntoPrimary(IntelInstructionList instructions, TemporaryVariableStack temporaryStack) throws CompileException {
        // Pops the temporary variable at the top of the stack into the primary register
        TemporaryVariable variable  = temporaryStack.peek();
        Register primary            = getPrimaryRegisterFromDataType(variable.type());
        return popTemporaryAndSignExtend(instructions, temporaryStack, variable, primary, PRIMARY_GENERAL_REGISTER);
    }

    private static Register popTemporaryAndSignExtend(final IntelInstructionList instructions, final TemporaryVariableStack temporaryStack,
                                                      final TemporaryVariable variable, final Register primary,
                                                      final Register primaryGeneralRegister) {
        Operation lastOp            = popIntoRegister(instructions, temporaryStack, primary);
        if(lastOp != MOVSX && variable.type().isInteger() && !(variable.type().isLong() || variable.type().isInt())){
            instructions.createSignExtend(primaryGeneralRegister, new Address<>(primary));
        }
        return primary;
    }

    private void createBinary(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {

        // Pops the two temporary values into the primary and secondary registers
        DataType result        = intermediate.result().type;
        Register secondary     = popTemporaryIntoSecondary(instructions, temporaryStack);
        Register primary       = popTemporaryIntoPrimary(instructions, temporaryStack);
        Operation op           = getOpFromResultType(intermediate.op(), result);

        // integer division is done via
        // idiv rcx, and the first is implicitly rax
        // mul similarly for integer is
        // mul rcx, and the first is implicitly rax
        if(intermediate.op() == IntermediateOperation.DIV && result.isInteger()){
            instructions.createIntegerDivision();
        } else if(intermediate.op() == IntermediateOperation.MUL && result.isInteger()){
            instructions.addInstruction(op, new Address<>(secondary));
        } else{
            instructions.addInstruction(op, primary, secondary);
        }
        addTemporary(instructions, temporaryStack, result);
    }
    private void createShift(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        // pops the two values and create the shift
        Operation op            = getBinaryOpFromIntermediateOp(intermediate.op());
        popTemporaryIntoSecondary(instructions, temporaryStack);
        Register primary        = popTemporaryIntoPrimary(instructions, temporaryStack);

        // shift is done via byte version of register, so in this case it's cl and not rcx
        instructions.addInstruction(op, primary, Register.getSecondaryRegisterFromDataType(DataType.getByte()));
        addTemporary(instructions, temporaryStack, intermediate.result().type);
    }
    private void createMod(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {

        // mod is integer division and instead take the remainder which is in rdx
        // So just pop both the temporaries, do integer division and store the remainder as a temporary
        DataType type = intermediate.result().type;
        popTemporaryIntoSecondary(instructions, temporaryStack);
        popTemporaryIntoPrimary(instructions, temporaryStack);
        instructions.createIntegerDivision();
        instructions.createMoveIntoPrimary(type, new Address<>(getThirdRegisterFromDataType(type)));
        addTemporary(instructions, temporaryStack, type);
    }
    private void createPostfixFloat(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Operation op, DataType result, boolean post) throws CompileException {

        // Get a immediate of 1 into the secondary register
        popTemporaryIntoPrimary(instructions, temporaryStack);
        instructions.createMoveIntoPrimary(result, Address.getEffectiveAddress(RAX));
        instructions.createMove(DataType.getInt(), new Address<>(SECONDARY_GENERAL_REGISTER), new Immediate<>(1));
        instructions.addInstruction(result.isDouble() ? CVTSI2SD : CVTSI2SS, SECONDARY_SSE_REGISTER, SECONDARY_GENERAL_REGISTER);

        // Then if we're doing ++foo we store the incremented/decremented value as a temporary
        // and if it's foo++ we do it afterwards
        op = op.convertToFloatingPoint(result);
        if(post){
            addTemporary(instructions, temporaryStack, result);
            instructions.postfixArithmeticFloat(op, result);
        }else{
            instructions.postfixArithmeticFloat(op, result);
            addTemporary(instructions, temporaryStack, result);
        }
    }
    private void createPostfixAndPrefix(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        DataType result     = intermediate.result().type;
        Operation op        = getBinaryOpFromIntermediateOp(intermediate.op());
        boolean post        = intermediate.op() == IntermediateOperation.POST_INC;

        // either floating point or general postfix/prefix
        if(result.isFloatingPoint()){
            createPostfixFloat(instructions, temporaryStack, op, result, post);
        }else{
            // is this pointer arithmetic or integer
            Operation pointerOp = op == INC ? ADD : SUB;
            createPostfixInt(instructions, temporaryStack, op, pointerOp, post, result);
        }
    }

    private void createPostfixInt(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Operation op, Operation pointerOp, boolean post, DataType target) throws CompileException {
        // pop the pointer of the value into secondary register
        // take the value the pointer points to into primary
        popTemporaryIntoSecondary(instructions, temporaryStack);
        Address<?> secondaryGeneralEffective = Address.getEffectiveAddress(SECONDARY_GENERAL_REGISTER);
        instructions.createMoveIntoPrimary(target, secondaryGeneralEffective);

        // if it's foo++ we store the temporary before the postfix
        if(post){
            addTemporary(instructions, temporaryStack, target);
        }

        // pointer arithmetic is an add with the size of the underlying struct and not an
        Address<?> primary = new Address<>(getPrimaryRegisterFromDataType(target));
        if(target.isPointer()){
            int pointerSize = symbolTable.getStructureSize(target.getTypeFromPointer());
            instructions.addInstruction(pointerOp, primary, new Immediate<>(pointerSize));
        }else{
            instructions.addInstruction(op, primary);
        }
        // Store it into the pointers that's still in secondary
        instructions.createMove(DataType.getInt(), secondaryGeneralEffective, primary);

        // if it's ++foo we store the temporary after the postfix
        if(!post){
            addTemporary(instructions, temporaryStack, target);
        }
    }
    private void createNegate(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        // a negate i.e -5 is a not and an inc
        Register primary    = popTemporaryIntoPrimary(instructions, temporaryStack);
        instructions.addInstruction(NOT, primary);
        instructions.addInstruction(INC, primary);
        addTemporary(instructions, temporaryStack, intermediate.result().type);
    }
    private void createLogical(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        // pop both values
        Register right  = popTemporaryIntoSecondary(instructions, temporaryStack);
        Register left   = popTemporaryIntoPrimary(instructions, temporaryStack);

        // get the value we need to match in order to short circuit the expression
        int shortCircuitValue = intermediate.op() == IntermediateOperation.LOGICAL_OR ? 1 : 0;

        // so compare versus the first value
        // if it's an OR (||) and the value is true we short circuit and jump to the merge label
        // otherwise we compare the last value with 1 and then the result of that comparison is our last value
        instructions.addInstruction(CMP, left, new Immediate<>(shortCircuitValue));
        String mergeLabel        = symbolTable.generateLabel().name;

        instructions.addInstruction(JE, new Immediate<>(mergeLabel));
        instructions.createCompare(CMP, SETE, right, new Immediate<>(1));
        instructions.addLabel(mergeLabel);
        addTemporary(instructions, temporaryStack, intermediate.result().type);
    }
    private void createComparison(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {

        // pop both values into the correct registers
        Register right          = popTemporaryIntoSecondary(instructions, temporaryStack);
        Register left           = popTemporaryIntoPrimary(instructions, temporaryStack);
        Operation op            = getOpFromResultType(intermediate.op(), intermediate.result().type);
        DataType destination    = intermediate.operand1().type;

        // convert the operation if needed
        // comparisons are done via doing a cmp -> SETXX value which sets a destination register to the result of a comparison
        // and in sse the vector registers have different set and compare operations
        if(destination.isFloatingPoint()){
            op = op.convertToFloat();
        }

        Operation cmpOp = getCmpOpFromType(destination);
        instructions.createCompare(cmpOp, op, left, new Address<>(right));
        addTemporary(instructions, temporaryStack, intermediate.result().type);
    }
    private void createNotFloat(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, DataType resultType) throws CompileException {

        // doing !foo where foo is a float is just a floating point comparison with the value 0
        Register primary        = popTemporaryIntoPrimary(instructions, temporaryStack);
        instructions.createMoveIntoPrimary(DataType.getInt(), new Immediate<>(0));

        Operation op        = getCmpOpFromType(resultType);
        Operation convertOp = getConvertOpFromType(DataType.getInt(), resultType);

        instructions.addInstruction(convertOp, SECONDARY_SSE_REGISTER, RAX);
        instructions.createCompare(op, SETE, primary, new Address<>(SECONDARY_SSE_REGISTER));
    }
    private void createNot(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        DataType intermediateType = intermediate.result().type;
        // a not (!foo) is just a comparison with 0 and setting the result
        if(intermediateType.isFloatingPoint()){
            createNotFloat(instructions, temporaryStack, intermediateType);
        }else{
            Register primary = popTemporaryIntoPrimary(instructions, temporaryStack);
            instructions.createCompare(CMP, SETE, primary, new Immediate<>(0));
        }
        addTemporary(instructions, temporaryStack, intermediateType);
    }
    private int createCallInternal(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Symbol functionSymbol) throws CompileException
    {
        FunctionSymbol function         = (FunctionSymbol) functionSymbol;
        int numberOfArguments           = function.getGeneralCount() + function.getFloatingPointCount();
        List<StructureField> arguments  = function.getArguments();
        int stackPointerOffset                = function.getStackSpace();

        // all internal arguments are passed via the stack
        // Since the arguments are all passed via the stack, the first argument popped is the last argument
        // the offset we store the argument at is the total space minus the size of prior arguments and its own size
        for(int i = numberOfArguments - 1 ; i >= 0; i--){
            DataType argumentType = arguments.get(i).type();
            stackPointerOffset -= this.symbolTable.getStructureSize(argumentType);

            // Struct needs to be moved differently since it's more then 8 bytes (maybe)
            if(argumentType.isStructure()){
                moveStruct(instructions, temporaryStack, argumentType, stackPointerOffset);
            }else{
                popTemporaryIntoPrimary(instructions, temporaryStack);
                instructions.createMove(argumentType, Address.getEffectiveAddress(RSP, stackPointerOffset), new Address<>(Register.getPrimaryRegisterFromDataType(argumentType)));
            }
        }

        return function.getStackSpace();
    }
    public int createCallExternal(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Symbol functionSymbol) throws CompileException {
        // this assumes arguments will be passed some (or none) in registers and the rest on the stack
        // this means we need to keep a count of how which argument we're currently trying to move

        Register[] floatingPointRegisters = this.callingConvention.getFloatingPointRegisters();
        Register[] generalRegisters = this.callingConvention.getGeneralRegisters();
        // Every argument is 8 bytes on 64bit :)
        final int argumentSize = 8;

        int stackSpace = 0;
        FunctionSymbol arguments    = (FunctionSymbol) functionSymbol;

        // the amount of stack space needed for an external function is the overflow of the number of floating point and general registers
        // times 8 since every argument is two bytes on 64 bit linux at least
        // We store the amount of stack space in order to make sure alignment is 16 bytes at all times
        int floatingPointCount      = arguments.getFloatingPointCount();
        if(floatingPointCount >= floatingPointRegisters.length){
            stackSpace += (floatingPointCount - floatingPointRegisters.length) * argumentSize;
        }

        int generalCount            = arguments.getGeneralCount();
        if(generalCount >= generalRegisters.length){
            stackSpace += (generalCount - generalRegisters.length) * argumentSize;
        }

        // note that we don't overwrite any argument since they're on the stack and when we pop we get them in reverse
        // So that any stack argument will be the first that gets popped and the last ones can be popped into the correct register
        // Note also that this doesn't mean that they got evaluated in that order

        int numberOfArguments = generalCount + floatingPointCount;
        for(int i = 0; i < numberOfArguments; i++){
            // pop the argument
            TemporaryVariable temporaryVariable = temporaryStack.peek();
            IntelCodeGenerator.popTemporaryIntoPrimary(instructions, temporaryStack);

            // calculate its position
            int stackPosition = (stackSpace - argumentSize) - argumentSize * i;
            Address<?> stackAddress = Address.getEffectiveAddress(RSP,stackPosition);

            // figure out what type it is and do the corresponding move
            if(temporaryVariable.type().isFloatingPoint()){
                floatingPointCount--;
                Address<?> primary = new Address<>(PRIMARY_SSE_REGISTER);
                boolean temporaryIsFloat = temporaryVariable.type().isFloat();

                // should be via the stack
                if(floatingPointCount >= floatingPointRegisters.length){
                    if(temporaryIsFloat){
                        instructions.addInstruction(CVTSS2SD, PRIMARY_SSE_REGISTER, PRIMARY_SSE_REGISTER);
                    }
                    instructions.createMove(DataType.getDouble(), stackAddress, primary);
                // since sse differentiates between float and double in vector registers we need to convert a float to a double if needed
                }else if(temporaryIsFloat){
                    instructions.addInstruction(CVTSS2SD, new Address<>(floatingPointRegisters[floatingPointCount]), PRIMARY_SSE_REGISTER);
                } else{
                    instructions.createMove(DataType.getDouble(), new Address<>(floatingPointRegisters[floatingPointCount]), primary);
                }
            }else{
                // move it either to the correct register or onto the stack via the primary register
                generalCount--;
                Address<?> primary = new Address<>(PRIMARY_GENERAL_REGISTER);
                if(generalCount >= generalRegisters.length){
                    instructions.createMove(temporaryVariable.type(), stackAddress, primary);
                }else{
                    instructions.createMove(temporaryVariable.type(), new Address<>(generalRegisters[generalCount]), primary);
                }
            }
        }
        return stackSpace;
    }

    private void createCall(IntelInstructionList instructions,TemporaryVariableStack temporaryStack,  Intermediate intermediate) throws CompileException {
        int allocateArgumentStackSpaceIndex = instructions.size();
        int stackSize;
        // Figure out which calling convention to follow
        if(this.symbolTable.functionIsExternal(intermediate.operand1().name)){
            stackSize = this.createCallExternal(instructions, temporaryStack, intermediate.operand1());
        }else{
            stackSize = this.createCallInternal(instructions, temporaryStack, intermediate.operand1());
        }

        // create the call instruction and handle stack alignment
        instructions.createCall(intermediate.operand1().name);
        stackSize       += Compiler.getStackAlignment(stackSize);
        if(stackSize != 0){
            instructions.deallocateStackSpace(stackSize);
            instructions.addInstruction(allocateArgumentStackSpaceIndex, new IntelInstruction(SUB, new Address<>(RSP), new Immediate<>(stackSize)));
        }

        // add the return value (if any) to the stack
        if(!intermediate.result().type.isVoid()){
            addTemporary(instructions, temporaryStack, intermediate.result().type);
        }
    }
    private Address<?> getImmediate(ImmediateSymbol immediateSymbol, DataType result){
        // immediates are either a constant (string and float) or just a label/integer immediate
        if(result.isString() || result.isFloatingPoint()) {
            Constant constant = symbolTable.getConstants().get(immediateSymbol.getValue());
            return new Address<>(constant.label(), !result.isString());
        }
        return new Address<>(immediateSymbol.getValue());
    }
    private void createLoadImmediate(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {

        // get the immediate
        DataType result                     = intermediate.result().type;
        Address<?> immediate                = getImmediate((ImmediateSymbol) intermediate.operand1(), result);

        // move it into the primary and store it as a temporary
        instructions.createMoveIntoPrimary(result, immediate);
        addTemporary(instructions, temporaryStack, result);
    }
    private void createLoad(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        VariableSymbol variable = (VariableSymbol) intermediate.operand1();
        DataType result         = intermediate.result().type;

        // Loads a variable from the stack, get the effective address to the variable and then load either it or a pointer to it
        Address<?> source = Address.getEffectiveAddress(RBP, variable.getOffset());
        if(variable.type.isStructure() || intermediate.op() == IntermediateOperation.LOAD_POINTER){
            instructions.createLoadEffectiveAddress(getPrimaryRegisterFromDataType(result), source);
        }else{
            instructions.createMoveIntoPrimary(variable.type, source);
        }
        addTemporary(instructions, temporaryStack, result);
    }
    private void createStoreArrayItem(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        ArrayItemSymbol arraySymbol = (ArrayItemSymbol) intermediate.operand1();
        ArrayDataType arrayDataType = (ArrayDataType)   arraySymbol.type;
        DataType itemType           = arrayDataType.getItemType();

        // get the address to the item
        // and move the item into the correct address
        Address<?> stackAddress = Address.getEffectiveAddress(RBP, arraySymbol.getOffset());
        if(itemType.isStructure()){
            // Structs are moved via the instructions "rep movsb" which moves a n number of bytes from rdi into rsi
            // where rdi and rsi are pointers
            popIntoRegister(instructions, temporaryStack, RSI);
            instructions.createLoadEffectiveAddress(RDI, stackAddress);
            instructions.createMoveStringByte(symbolTable.getStructureSize(itemType));
        }else{
            popTemporaryIntoPrimary(instructions, temporaryStack);
            instructions.createMove(itemType, stackAddress, new Address<>(getPrimaryRegisterFromDataType(itemType)));
        }

    }
    private void createImmediateMultiply(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        // pop a temporary, multiply it with an immediate and then push it again
        ImmediateSymbol immediateSymbol = (ImmediateSymbol) intermediate.operand2();
        Register primary                = popTemporaryIntoPrimary(instructions, temporaryStack);
        instructions.addInstruction(IMUL, primary, new Immediate<>(immediateSymbol.getValue()));
        addTemporary(instructions, temporaryStack, intermediate.result().type);
    }
    private void createAssign(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        DataType targetType = intermediate.operand1().type;
        if(targetType.isStructure()){
            // since the stored temporary for a struct is a pointer, just pop the pointers to the structure and the variable into rsi and rdi
            // and do "rep movsb" with the size of the struct
            popIntoRegister(instructions, temporaryStack, RDI);
            popIntoRegister(instructions, temporaryStack, RSI);
            instructions.createMoveStringByte(symbolTable.getStructureSize(targetType));
        }else{
            // pop both values with the value into primary and pointer to the variable in secondary
            // then move the value from rax into the effective of rcx
            TemporaryVariable pointer  = temporaryStack.pop();
            TemporaryVariable value    = temporaryStack.pop();
            instructions.createMoveIntoPrimary(value.type(), Address.getEffectiveAddress(RBP, value.offset()));
            instructions.createMove(pointer.type(), new Address<>(getSecondaryRegisterFromDataType(pointer.type())), Address.getEffectiveAddress(RBP, pointer.offset()));
            instructions.createMove(targetType, Address.getEffectiveAddress(SECONDARY_GENERAL_REGISTER), new Address<>(getPrimaryRegisterFromDataType(targetType)));
        }
    }
    private void createJumpOnCondition(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        Symbol dest             = intermediate.operand1();
        // create a jump on either true or false
        // this is done via creating a comparison to either 1 or 0 (true or false)
        int jumpOnTrue          = intermediate.op() == IntermediateOperation.JMP_T ? 1 : 0;
        popTemporaryIntoPrimary(instructions, temporaryStack);

        Operation cmpOp = getCmpOpFromType(dest.type);
        instructions.addInstruction(cmpOp, PRIMARY_GENERAL_REGISTER, new Immediate<>(jumpOnTrue));
        instructions.addInstruction(JE, new Immediate<>(dest.name));
    }
    private void createDereference(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        // dereference is just loading the value that a pointer points to
        // looks like rax, [rax]
        DataType result     = intermediate.result().type;
        Register primary    = popTemporaryIntoPrimary(instructions, temporaryStack);
        instructions.createMoveIntoPrimary(result, Address.getEffectiveAddress(primary));
        addTemporary(instructions, temporaryStack, result);
    }
    private void createIndex(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        DataType target = intermediate.result().type;

        // pop the index value and the target
        popTemporaryIntoPrimary(instructions, temporaryStack);
        popTemporaryIntoSecondary(instructions, temporaryStack);

        // figure out what size the indexed value needs to be increased by (i.e the size of the type)
        int size = symbolTable.getStructureSize(intermediate.operand1().type.getTypeFromPointer());
        instructions.addInstruction(IMUL, PRIMARY_GENERAL_REGISTER, new Immediate<>(size));
        instructions.addInstruction(ADD, PRIMARY_GENERAL_REGISTER, SECONDARY_GENERAL_REGISTER);

        // this gets called by REFERENCE_INDEX as well which stores the pointer and not the value it points to
        // if we have INDEX we get the value in question
        if(intermediate.op() == IntermediateOperation.INDEX){
            Register destination = getPrimaryRegisterFromDataType(target);
            Address<?> source = Address.getEffectiveAddress(PRIMARY_GENERAL_REGISTER);
            if(target.isStructure()){
                instructions.createLoadEffectiveAddress(destination, source);
            }else{
                instructions.createMove(target, new Address<>(destination), source);
            }
        }

        addTemporary(instructions, temporaryStack, target);
    }
    private void createLoadMember(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        DataType result             = intermediate.result().type;
        MemberSymbol member         = (MemberSymbol) intermediate.operand1();
        Address<?> memberAddress    = Address.getEffectiveAddress(PRIMARY_GENERAL_REGISTER, member.getOffset());

        // pop the pointer to the struct and then load the value (or pointer to it) into the primary register
        popTemporaryIntoPrimary(instructions, temporaryStack);
        if(result.isStructure() || intermediate.op() == IntermediateOperation.LOAD_MEMBER_POINTER){
            instructions.createLoadEffectiveAddress(PRIMARY_GENERAL_REGISTER, memberAddress);
        }else{
            instructions.createMoveIntoPrimary(result, memberAddress);
        }
        addTemporary(instructions, temporaryStack, result);
    }
    private void moveStruct(IntelInstructionList instructions, TemporaryVariableStack temporaryStack,DataType structureType, int offset){
        // move struct is done via "rep movsb" i.e "repeat move string bytes". Which takes an amount of bytes (stored in rcx)
        // and moves it from rdi into rsi
        popIntoRegister(instructions, temporaryStack, RSI);
        instructions.createLoadEffectiveAddress(RDI, Address.getEffectiveAddress(RSP, offset));
        instructions.createMoveStringByte(symbolTable.getStructureSize(structureType));
    }
    private void createConvert(IntelInstructionList instructions, TemporaryVariableStack temporaryStack, Intermediate intermediate) throws CompileException {
        DataType dest       = intermediate.operand1().type;
        DataType result     = intermediate.result().type;
        // we don't need to convert if it's from an int (4 bytes) to a long (8 bytes)
        // or if the target is of a smaller type i.e long -> byte
        // that's because if will get referred to by that size afterwards so truncating doesn't matter
        if(dest.isInteger() && result.isInteger() && dest.isSameType(DataType.getHighestDataTypePrecedence(dest, result))){
            return;
        }
        popTemporaryIntoPrimary(instructions, temporaryStack);

        Operation convert   = getConvertOpFromType(dest, result);
        Register target         = getMinimumConvertTarget(convert, result);
        Register source         = getMinimumConvertSource(convert, dest);
        instructions.addInstruction(convert, target, source);
        addTemporary(instructions, temporaryStack, result);
    }


    private void buildConstantsString(SymbolTable symbolTable, StringBuilder stringBuilder){
        stringBuilder.append("\n\n");
        for(Map.Entry<String, Constant> entry : symbolTable.getConstants().entrySet()){
            Constant value = entry.getValue();
            if(value.type() == DataTypes.STRING){
                // Need to replace \n with 10 which is ASCII for new line
                // Should be done for other escape characters as well
                String formatted = entry.getKey().replace("\\n", "\", 10, \"");
                stringBuilder.append(String.format("%s db \"%s\", 0\n", value.label(), formatted));
            }else{
                // this is a float constant
                stringBuilder.append(String.format("%s dd %s\n", value.label(), entry.getKey()));
            }
        }
    }
    public String createOutputString(SymbolTable symbolTable, Map<String, ? extends InstructionList> functions) {
        StringBuilder stringBuilder = new StringBuilder();

        // define the main and all extern functions
        stringBuilder.append("global _start\n");
        Map<String, Function> externalFunctions = symbolTable.getExternalFunctions();
        for(String function : externalFunctions.keySet()){
            stringBuilder.append(String.format("extern %s\n", function));
        }

        // output constants
        buildConstantsString(symbolTable, stringBuilder);

        // output a token main that calls into main and stores the return value
        stringBuilder.append("\n\nsection .text\n_start:\ncall main\nmov rbx, rax\nmov rax, 1\nint 0x80\n");

        // create an output string for every function
        for(Map.Entry<String, ? extends InstructionList> function : functions.entrySet()){
            String name                     = function.getKey();
            InstructionList instructions    = function.getValue();

            stringBuilder.append(String.format("\n\n%s:\n", name));
            for(Instruction instruction: instructions){
                stringBuilder.append(String.format("%s\n", instruction.emit()));
            }
        }
        return stringBuilder.toString();
    }
    public IntelCodeGenerator(SymbolTable symbolTable, T callingConvention){
        this.symbolTable = symbolTable;
        this.callingConvention = callingConvention;
    }
}