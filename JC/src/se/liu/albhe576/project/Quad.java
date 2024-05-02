package se.liu.albhe576.project;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result) {
    @Override
    public String toString() {
        return String.format("%s %s %s %s",op.name(),operand1,operand2,result);
    }

    public static Symbol convertType(SymbolTable symbolTable, QuadList quads, Symbol symbol, DataType target){
        if(!symbol.type.isSameType(target)){
            symbol = quads.createConvert(symbolTable, symbol, target);
        }
        return symbol;
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

    public void convertAddress(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack) throws CompileException {
        if(operand1.type.isInteger() && result.type.isInteger() && operand1.type.isSameType(DataType.getHighestDataTypePrecedence(operand1.type, result.type))){
            return;
        }
        Operand<Register> primary = popTemporaryIntoPrimary(instructions, tempStack);
        if(!(operand1.type.isLong() || operand1.type.isInt()) && operand1.type.isInteger()){
            instructions.add(new Instruction<>(Operation.MOVSX, new Operand<>(Register.RAX), primary));
        }

        Operation convert = getConvertOpFromType(operand1.type, result.type);
        Register target = getMinimumConvertTarget(convert, result.type);
        Register source = getMinimumConvertSource(convert, operand1.type);
        instructions.add(new Instruction<>(convert, new Operand<>(target), new Operand<>(source)));
        addTemporary(instructions, tempStack);
    }
    private void createMovSB(List<Instruction<?, ?>> instructions, int immediate){
        instructions.add(new Instruction<>(Operation.MOV, new Operand<>(Register.RCX), new Operand<>(immediate)));
        instructions.add(new Instruction<>(Operation.REP, new Operand<>(Operation.MOVSB), null));

    }
    private void assignStruct(List<Instruction<?, ?>> instructions, SymbolTable symbolTable,TemporaryVariableStack tempStack){
        popIntoRegister(instructions, tempStack, new Operand<>(Register.RDI));
        popIntoRegister(instructions, tempStack, new Operand<>(Register.RSI));
        int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), result.type);
        this.createMovSB(instructions, structSize);
    }

    private void moveStruct(List<Instruction<?, ?>> instructions, SymbolTable symbolTable,TemporaryVariableStack tempStack){
        popIntoRegister(instructions, tempStack, new Operand<>(Register.RSI));
        ArgumentSymbol argumentSymbol = (ArgumentSymbol) operand2;
        instructions.add(new Instruction<>(Operation.LEA, new Operand<>(Register.RDI), new Operand<>(Register.RSP, true, argumentSymbol.getOffset())));

        int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), operand1.type);
        this.createMovSB(instructions, structSize);
    }
    private static final Instruction<?, ?>[] EPILOGUE_INSTRUCTIONS = new Instruction[]{
            new Instruction<>(Operation.MOV, new Operand<>(Register.RSP), new Operand<>(Register.RBP)),
            new Instruction<>(Operation.POP, new Operand<>(Register.RBP), null)
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
    private void createShift(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack, Operation op) throws CompileException {
        this.popTemporaryIntoSecondary(instructions, tempStack);
        Operand<Register> primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
        instructions.add(new Instruction<>(op, primary, new Operand<>(Register.CL)));
        this.addTemporary(instructions, tempStack);
    }

    private void createBinary(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack, Operation op) throws CompileException {
        Operand<Register> secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
        Operand<Register> primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
        instructions.add(new Instruction<>(op, primary, secondary));
        this.addTemporary(instructions, tempStack);
    }
    private static <L, R> void  createCompare(List<Instruction<?, ?>> instructions, Operation compareOp, Operation setOp, Operand<L> left, Operand<R> right){
        instructions.add(new Instruction<>(compareOp, left, right));
        instructions.add(new Instruction<>(setOp, new Operand<>(Register.AL), null));
        instructions.add(new Instruction<>(Operation.MOVSX, new Operand<>(Register.RAX), new Operand<>(Register.AL)));
    }
    private void createComparison(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack, Operation op) throws CompileException {
        Operand<Register> right = popTemporaryIntoSecondary(instructions, tempStack);
        Operand<Register> left = popTemporaryIntoPrimary(instructions, tempStack);
        createCompare(instructions, getCmpOpFromType(operand1.type), op, left, right);
        this.addTemporary(instructions, tempStack);
    }

    private Operand<Register> popIntoRegister(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack, Operand<Register> primary){
        VariableSymbol value            = tempStack.popVariable();
        Operation moveOp                = getMoveOpFromType(value.type);
        instructions.add(new Instruction<>(moveOp, primary, new Operand<>(Register.RBP, true, value.offset)));
        return primary;

    }
    private Operand<Register> popTemporaryIntoSecondary(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack){
        VariableSymbol value            = tempStack.peek();
        Operand<Register> primary                 = new Operand<>(Register.getSecondaryRegisterFromDataType(value.type));
        Operand<Register> out = popIntoRegister(instructions, tempStack, new Operand<>(primary.getAddress()));
        if(value.type.isInteger() && !value.type.isLong()){
            instructions.add(new Instruction<>(Operation.MOVSX, new Operand<>(Register.RCX), primary));
        }
        return out;
    }
    private Operand<Register> popTemporaryIntoPrimary(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack){
        VariableSymbol value            = tempStack.peek();
        Operand<Register> primary                 = new Operand<>(Register.getPrimaryRegisterFromDataType(value.type));
        Operand<Register> out = popIntoRegister(instructions, tempStack, new Operand<>(primary.getAddress()));
        if(value.type.isInteger() && !value.type.isLong()){
            instructions.add(new Instruction<>(Operation.MOVSX, new Operand<>(Register.RAX), primary));
        }
        return out;
    }
    private void addTemporary(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack) throws CompileException {
        int offset;
        if(result.type.isStruct()){
            offset = tempStack.pushVariable(result.name, result.type.getPointerFromType());
        }else{
            offset = tempStack.pushVariable(result.name, result.type);
        }
        Operation moveOp = getMoveOpFromType(result.type);
        instructions.add(new Instruction<>(moveOp, new Operand<>(Register.RBP, true, offset), new Operand<>(Register.getPrimaryRegisterFromDataType(result.type))));
    }

    public static final Register[] LINUX_FLOAT_PARAM_LOCATIONS = new Register[]{Register.XMM0, Register.XMM1, Register.XMM2, Register.XMM3, Register.XMM4, Register.XMM5, Register.XMM6, Register.XMM7};
    public static final Register[] LINUX_GENERAL_PARAM_LOCATIONS = new Register[]{Register.RDI, Register.RSI, Register.RDX, Register.RCX, Register.R8, Register.R9};

    private void addExternalParameter(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack, ArgumentSymbol argumentSymbol) {

        VariableSymbol param = tempStack.peek();
        popTemporaryIntoPrimary(instructions, tempStack);
        Register[] registers = operand1.type.isFloatingPoint() ? LINUX_FLOAT_PARAM_LOCATIONS : LINUX_GENERAL_PARAM_LOCATIONS;
        if(argumentSymbol.getCount() >= registers.length){
            // Push it onto the stack
            instructions.add(new Instruction<>(getMoveOpFromType(operand1.type), new Operand<>(Register.RSP, true, argumentSymbol.getOffset()), new Operand<>(Register.getPrimaryRegisterFromDataType(operand1.type))));
        }else{
            Operand<Register> target = new Operand<>(registers[argumentSymbol.getCount()]);
            Register source = param.type.isFloatingPoint() ? Register.PRIMARY_SSE_REGISTER : Register.PRIMARY_GENERAL_REGISTER;
            instructions.add(new Instruction<>(getMoveOpFromType(param.type), target, new Operand<>(source)));
            // move it into the register
        }
    }
    private void createLogical(SymbolTable symbolTable, List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack) throws CompileException {
        Operand<Register> right = popTemporaryIntoSecondary(instructions, tempStack);
        Operand<Register> left = popTemporaryIntoPrimary(instructions, tempStack);
        int firstRegister = this.op == QuadOp.LOGICAL_OR ? 1 : 0;

        instructions.add(new Instruction<>(Operation.CMP, left, new Operand<>(firstRegister)));
        String mergeLabel        = symbolTable.generateLabel().name;

        instructions.add(new Instruction<>(Operation.JE, new Operand<>(mergeLabel, false), null));
        createCompare(instructions, Operation.CMP, Operation.SETE, right, new Operand<>(1));
        instructions.add(new Instruction<>(mergeLabel));
        addTemporary(instructions, tempStack);
    }
    private void calculateIndex(SymbolTable symbolTable, List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack) throws CompileException {
        popTemporaryIntoPrimary(instructions, tempStack);
        popTemporaryIntoSecondary(instructions, tempStack);

        int size = symbolTable.getStructSize(operand1.type.getTypeFromPointer());
        instructions.add(new Instruction<>(Operation.IMUL, new Operand<>(Register.PRIMARY_GENERAL_REGISTER), new Operand<>(size)));
        instructions.add(new Instruction<>(Operation.ADD, new Operand<>(Register.PRIMARY_GENERAL_REGISTER), new Operand<>(Register.SECONDARY_GENERAL_REGISTER)));
    }

    private void createJumpOnCondition(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack){
        Operand<Integer> immediate = new Operand<>(this.op == QuadOp.JMP_F ? 0 : 1);
        popTemporaryIntoPrimary(instructions, tempStack);
        instructions.add(new Instruction<>(getCmpOpFromType(operand1.type), new Operand<>(Register.PRIMARY_GENERAL_REGISTER), immediate));
        instructions.add(new Instruction<>(Operation.JE, new Operand<>(operand1.name, false), null));
    }

    private static final Map<QuadOp, Operation> GENERAL_QUAD_OP_TO_BINARY_OP_MAP = Map.ofEntries(
            Map.entry(QuadOp.ADD_I, Operation.ADD),
            Map.entry(QuadOp.AND, Operation.AND),
            Map.entry(QuadOp.OR, Operation.OR),
            Map.entry(QuadOp.XOR, Operation.XOR),
            Map.entry(QuadOp.SUB_I, Operation.SUB)
    );
    private static final Map<QuadOp, Operation> GENERAL_QUAD_OP_TO_COMPARISON_OP_MAP = Map.ofEntries(
            Map.entry(QuadOp.LESS_I, Operation.SETL),
            Map.entry(QuadOp.LESS_EQUAL_I, Operation.SETLE),
            Map.entry(QuadOp.GREATER_I, Operation.SETG),
            Map.entry(QuadOp.GREATER_EQUAL_I, Operation.SETGE),
            Map.entry(QuadOp.EQUAL_I, Operation.SETE),
            Map.entry(QuadOp.EQUAL_F, Operation.SETE),
            Map.entry(QuadOp.NOT_EQUAL_F, Operation.SETNE),
            Map.entry(QuadOp.NOT_EQUAL_I, Operation.SETNE),
            Map.entry(QuadOp.LESS_F, Operation.SETB),
            Map.entry(QuadOp.LESS_EQUAL_F, Operation.SETBE),
            Map.entry(QuadOp.GREATER_F, Operation.SETA),
            Map.entry(QuadOp.GREATER_EQUAL_F, Operation.SETAE)
    );

    public List<Instruction<?, ?>> emitInstructions(SymbolTable symbolTable, Map<String, Constant> constants,TemporaryVariableStack tempStack) throws CompileException{
        List<Instruction<?, ?>> instructions = new ArrayList<>();
        switch(op){
            case ADD_I, SUB_I, AND, OR, XOR:{
                this.createBinary(instructions, tempStack, GENERAL_QUAD_OP_TO_BINARY_OP_MAP.get(this.op));
                break;
            }
            case LESS_I, LESS_EQUAL_I, GREATER_I, GREATER_EQUAL_I, EQUAL_I, EQUAL_F,
                 NOT_EQUAL_I, NOT_EQUAL_F, LESS_F, LESS_EQUAL_F, GREATER_F, GREATER_EQUAL_F:{
                this.createComparison(instructions, tempStack, GENERAL_QUAD_OP_TO_COMPARISON_OP_MAP.get(this.op));
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
            case ALLOCATE:{
                instructions.add(new Instruction<>(Operation.SUB, new Operand<>(Register.RSP), new Operand<>(operand1.name)));
                break;
            }
            case LOGICAL_AND, LOGICAL_OR:{
                this.createLogical(symbolTable, instructions, tempStack);
                break;
            }
            case PARAM:{
                if(operand1.type.isStruct()){
                    this.moveStruct(instructions, symbolTable, tempStack);
                }else {
                    ArgumentSymbol argumentSymbol = (ArgumentSymbol) operand2;
                    if(!argumentSymbol.getExternal()){
                        popTemporaryIntoPrimary(instructions, tempStack);
                        instructions.add(new Instruction<>(getMoveOpFromType(operand1.type), new Operand<>(Register.RSP, true, argumentSymbol.getOffset()), new Operand<>(Register.getPrimaryRegisterFromDataType(operand1.type))));
                    }else{
                        this.addExternalParameter(instructions, tempStack, argumentSymbol);
                    }
                }
                break;
            }
            case CALL:{
                instructions.add(new Instruction<>(Operation.CALL, new Operand<>(operand1.name), null));

                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                int stackSize = Integer.parseInt(immediateSymbol.getValue());
                stackSize += Compiler.getStackAlignment(stackSize);
                if(stackSize != 0){
                    instructions.add(new Instruction<>(Operation.ADD, new Operand<>(Register.RSP), new Operand<>(stackSize)));
                }
                if(!result.type.isVoid()){
                   addTemporary(instructions, tempStack);
                }
                break;
            }
            case JMP :{
                instructions.add(new Instruction<>(Operation.JMP, new Operand<>(operand1.name), null));
                break;
            }
            case LOAD, LOAD_POINTER:{
                VariableSymbol variable = (VariableSymbol) operand1;
                Operation op = (operand1.type.isStruct() || this.op == QuadOp.LOAD_POINTER) ? Operation.LEA : getMoveOpFromType(operand1.type);
                instructions.add(new Instruction<>(op, new Operand<>(Register.getPrimaryRegisterFromDataType(result.type)), new Operand<>(Register.RBP, true, variable.offset)));
                addTemporary(instructions, tempStack);
                break;
            }
            case CONVERT:{
                this.convertAddress(instructions,tempStack);
                break;
            }
            case LOAD_MEMBER, LOAD_MEMBER_POINTER:{
                ImmediateSymbol memberSymbol    = (ImmediateSymbol) operand2;
                popTemporaryIntoPrimary(instructions, tempStack);
                Operation operation = (result.type.isStruct() || this.op == QuadOp.LOAD_MEMBER_POINTER) ? Operation.LEA : getMoveOpFromType(result.type);
                instructions.add(new Instruction<>(operation, new Operand<>(Register.getPrimaryRegisterFromDataType(result.type)), new Operand<>(Register.RAX, true, Integer.parseInt(memberSymbol.getValue()))));
                addTemporary(instructions, tempStack);
                break;
            }
            case STORE_ARRAY_ITEM:{
                VariableSymbol arraySymbol = (VariableSymbol) operand1;
                ArrayDataType arrayDataType = (ArrayDataType)arraySymbol.type;
                String index = ((ImmediateSymbol)operand2).getValue();
                int offset = arraySymbol.offset + Integer.parseInt(index);
                if(arrayDataType.itemType.isStruct()){
                    popIntoRegister(instructions, tempStack, new Operand<>(Register.RSI));
                    instructions.add(new Instruction<>(Operation.LEA, new Operand<>(Register.RDI), new Operand<>(Register.RBP, true, offset)));

                    this.createMovSB(instructions, SymbolTable.getStructSize(symbolTable.getStructs(), arrayDataType.itemType));
                    break;
                }

                popTemporaryIntoPrimary(instructions, tempStack);
                Operation moveOp = getMoveOpFromType(arrayDataType.itemType);
                instructions.add(new Instruction<>(moveOp, new Operand<>(Register.RBP, true, offset), new Operand<>(Register.getPrimaryRegisterFromDataType(arrayDataType.itemType))));
                break;
            }
            case REFERENCE_INDEX, INDEX:{
                this.calculateIndex(symbolTable, instructions, tempStack);
                if(this.op == QuadOp.INDEX){
                    Operation operation = result.type.isStruct() ? Operation.LEA : getMoveOpFromType(result.type);
                    instructions.add(new Instruction<>(operation, new Operand<>(Register.getPrimaryRegisterFromDataType(result.type)), new Operand<>(Register.PRIMARY_GENERAL_REGISTER, true)));
                }
                addTemporary(instructions, tempStack);
                break;
            }
            case DEREFERENCE:{
                Operand<Register> primary = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction<>(getMoveOpFromType(result.type), new Operand<>(Register.getPrimaryRegisterFromDataType(result.type)), new Operand<>(primary.getAddress(), true)));
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
            case PRE_INC_I, POST_INC_I:{
                this.createPostfixInteger(instructions, tempStack, Operation.INC, Operation.ADD, this.op == QuadOp.POST_INC_I);
                break;
            }
            case PRE_DEC_I, POST_DEC_I:{
                this.createPostfixInteger(instructions, tempStack, Operation.DEC, Operation.SUB, this.op == QuadOp.POST_DEC_I);
                break;
            }
            case MUL_I:{
                this.popTemporaryIntoPrimary(instructions, tempStack);
                Operand<Register> secondary = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction<>(Operation.MUL, secondary, null));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case DIV_I:{
                Operand<Register> secondary = this.popTemporaryIntoSecondary(instructions, tempStack);
                this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction<>(Operation.XOR, new Operand<>(Register.RDX), new Operand<>(Register.RDX)));
                instructions.add(new Instruction<>(Operation.IDIV, secondary, null));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case SHL, SHR:{
                this.createShift(instructions, tempStack, this.op == QuadOp.SHL ? Operation.SHL : Operation.SHR);
                break;
            }
            case NEGATE:{
                Operand<Register> primary = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction<>(Operation.NOT, primary, null));
                instructions.add(new Instruction<>(Operation.INC, primary, null));
                addTemporary(instructions, tempStack);
                break;
            }
            case NOT_I :{
                Operand<Register> primary = popTemporaryIntoPrimary(instructions, tempStack);
                createCompare(instructions, Operation.CMP, Operation.SETE, primary, new Operand<>(0));
                addTemporary(instructions, tempStack);
                break;
            }
            case NOT_F :{
                Operand<Register> primary = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction<>(Operation.MOV, new Operand<>(Register.RAX), new Operand<>(0)));
                Operation op = getCmpOpFromType(operand1.type);
                Operation moveOp = getConvertOpFromType(DataType.getInt(), operand1.type);
                instructions.add(new Instruction<>(moveOp, new Operand<>(Register.SECONDARY_SSE_REGISTER), new Operand<>(Register.RAX)));
                createCompare(instructions, op, Operation.SETE, primary, new Operand<>(Register.SECONDARY_SSE_REGISTER));
                addTemporary(instructions, tempStack);
                break;
            }
            case MOD:{
                instructions.add(new Instruction<>(Operation.XOR, new Operand<>(Register.RDX), new Operand<>(Register.RDX)));
                Operand<Register> secondary = popIntoRegister(instructions,  tempStack, new Operand<>(Register.getSecondaryRegisterFromDataType(operand2.type)));
                popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction<>(Operation.IDIV, secondary, null));
                instructions.add(new Instruction<>(getMoveOpFromType(result.type), new Operand<>(Register.getPrimaryRegisterFromDataType(result.type)), new Operand<>(Register.getThirdRegisterFromDataType(result.type))));
                addTemporary(instructions, tempStack);
                break;
            }
            case LABEL:{
                instructions.add(new Instruction<>(operand1.name));
                break;
            }
            case RET_I, RET_F:{
                if(result != null){
                    this.popTemporaryIntoPrimary(instructions, tempStack);
                }
                instructions.addAll(List.of(EPILOGUE_INSTRUCTIONS));
                instructions.add(new Instruction<>(Operation.RET, null, null));
                break;
            }
            case LOAD_IMM_I, LOAD_IMM_F:{
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                if(result.type.isString() || result.type.isFloatingPoint()) {
                    Constant constant = constants.get(immediateSymbol.getValue());
                    instructions.add(new Instruction<>(getMoveOpFromType(result.type), new Operand<>(Register.getPrimaryRegisterFromDataType(result.type)), new Operand<>(constant.label(), !result.type.isString())));
                }else{
                    instructions.add(new Instruction<>(getMoveOpFromType(result.type), new Operand<>(Register.getPrimaryRegisterFromDataType(result.type)), new Operand<>(immediateSymbol.getValue())));
                }
                this.addTemporary(instructions, tempStack);
                break;
            }
            case IMUL:{
                Operand<Register> primary = popTemporaryIntoPrimary(instructions, tempStack);
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand2;
                instructions.add(new Instruction<>(Operation.IMUL, primary, new Operand<>(immediateSymbol.getValue())));
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
                    instructions.add(new Instruction<>(getMoveOpFromType(value.type), new Operand<>(Register.getPrimaryRegisterFromDataType(value.type)), new Operand<>(Register.RBP, true, value.offset)));
                    instructions.add(new Instruction<>(getMoveOpFromType(pointer.type), new Operand<>(Register.getSecondaryRegisterFromDataType(pointer.type)), new Operand<>(Register.RBP, true, pointer.offset)));
                    instructions.add(new Instruction<>(getMoveOpFromType(operand1.type), new Operand<>(Register.RCX, true), new Operand<>(Register.getPrimaryRegisterFromDataType(operand1.type))));
                }

                break;
            }
            default : {throw new CompileException(String.format("Don't know how to make instructions from %s", op.name()));}
        }
        return instructions;
    }

    private void setupPostfixFloat(TemporaryVariableStack tempStack, List<Instruction<?, ?>> instructions) {
        popTemporaryIntoPrimary(instructions, tempStack);

        Operand<Register> primary = new Operand<>(Register.getPrimaryRegisterFromDataType(operand1.type));
        Operation moveOp = getMoveOpFromType(result.type);
        instructions.add(new Instruction<>(moveOp, primary, new Operand<>(Register.RAX, true)));

        instructions.add(new Instruction<>(Operation.MOV, new Operand<>(Register.SECONDARY_GENERAL_REGISTER), new Operand<>(1)));
        Operation convertOp = result.type.isDouble() ? Operation.CVTSI2SD : Operation.CVTSI2SS;
        instructions.add(new Instruction<>(convertOp, new Operand<>(Register.SECONDARY_SSE_REGISTER), new Operand<>(Register.SECONDARY_GENERAL_REGISTER)));
    }
    private void immediateArithmeticFloat(List<Instruction<?, ?>> instructions,  Operation arithmeticOp){
        Operation moveOp = getMoveOpFromType(result.type);
        instructions.add(new Instruction<>(arithmeticOp, new Operand<>(Register.PRIMARY_SSE_REGISTER), new Operand<>(Register.SECONDARY_SSE_REGISTER)));
        instructions.add(new Instruction<>(moveOp, new Operand<>(Register.RAX, true), new Operand<>(Register.PRIMARY_SSE_REGISTER)));
    }
    private void createPostfixInteger(List<Instruction<?, ?>> instructions, TemporaryVariableStack tempStack, Operation op,Operation pointerOp, boolean post) throws CompileException {
        this.popTemporaryIntoSecondary(instructions, tempStack);
        Operand<Register> primary = new Operand<>(Register.getPrimaryRegisterFromDataType(operand1.type));
        instructions.add(new Instruction<>(Operation.MOV, primary, new Operand<>(Register.RCX, true)));
        if(post){
            this.addTemporary(instructions, tempStack);
        }
        if(operand1.type.isPointer()){
            instructions.add(new Instruction<>(pointerOp, primary, new Operand<>(operand1.type.getTypeFromPointer().getSize())));
        }else{
            instructions.add(new Instruction<>(op, primary, null));
        }
        instructions.add(new Instruction<>(Operation.MOV, new Operand<>(Register.RCX, true), primary));

        if(!post){
            this.addTemporary(instructions, tempStack);
        }
    }
}
