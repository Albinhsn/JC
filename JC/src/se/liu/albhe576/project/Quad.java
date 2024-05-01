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
        if(!operand1.type.isLong() && operand1.type.isInteger()){
            instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), primary));
        }

        Operation convert = getConvertOpFromType(operand1.type, result.type);
        Register target = getMinimumConvertTarget(convert, result.type);
        Register source = getMinimumConvertSource(convert, operand1.type);
        instructions.add(new Instruction(convert, new Address(target), new Address(source)));
        addTemporary(instructions, tempStack);
    }
    private void assignStruct(List<Instruction> instructions, SymbolTable symbolTable,TemporaryVariableStack tempStack){
        // Pop both pointers
        // target is rdi
        popIntoRegister(instructions, tempStack, new Address(Register.RDI));
        // source is rsi
        popIntoRegister(instructions, tempStack, new Address(Register.RSI));

        // Figure out how many bytes to move
        // rep movsb
        int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), result.type);
        instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX), new Immediate(structSize)));
        instructions.add(new Instruction(Operation.REP, new Address(Register.MOVSB), null));
    }

    private void moveStruct(List<Instruction> instructions, SymbolTable symbolTable,TemporaryVariableStack tempStack){

        popIntoRegister(instructions, tempStack, new Address(Register.RSI));
        ArgumentSymbol argumentSymbol = (ArgumentSymbol) operand2;
        instructions.add(new Instruction(Operation.LEA, new Address(Register.RDI), new Address(Register.RSP, true, argumentSymbol.offset)));

        int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), operand1.type);
        instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX), new Immediate(structSize)));
        instructions.add(new Instruction(Operation.REP, new Address(Register.MOVSB), null));

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
        if(type.isInteger() && source.isFloat()){
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

    private Address popIntoRegister(List<Instruction> instructions, TemporaryVariableStack tempStack, Address primary){
        VariableSymbol value            = tempStack.popVariable();
        Operation moveOp                = getMoveOpFromType(value.type);
        instructions.add(new Instruction(moveOp, primary, new Address(Register.RBP, true, value.offset)));
        return primary;

    }
    private Address popTemporaryIntoSecondary(List<Instruction> instructions, TemporaryVariableStack tempStack){
        VariableSymbol value            = tempStack.peek();
        Address primary                 = new Address(Register.getSecondaryRegisterFromDataType(value.type));
        Address out = popIntoRegister(instructions, tempStack, new Address(primary.register));
        if(value.type.isInteger() && !value.type.isLong()){
            instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RCX), primary));
        }
        return out;
    }
    private Address popTemporaryIntoPrimary(List<Instruction> instructions, TemporaryVariableStack tempStack){
        VariableSymbol value            = tempStack.peek();
        Address primary                 = new Address(Register.getPrimaryRegisterFromDataType(value.type));
        Address out = popIntoRegister(instructions, tempStack, new Address(primary.register));
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
        if(argumentSymbol.count >= registers.length){
            // Push it onto the stack
            throw new CompileException("Pls fix :)");
        }else{
            Address target = new Address(registers[argumentSymbol.count]);
            Register source = param.type.isFloatingPoint() ? Register.PRIMARY_SSE_REGISTER : Register.PRIMARY_GENERAL_REGISTER;
            instructions.add(new Instruction(getMoveOpFromType(param.type), target, new Address(source)));
            // move it into the register
        }
    }

    public List<Instruction> emitInstructions(SymbolTable symbolTable, Map<String, Constant> constants,TemporaryVariableStack tempStack) throws CompileException{
        List<Instruction> instructions = new ArrayList<>();
        switch(op){
            case ALLOCATE:{
                instructions.add(new Instruction(Operation.SUB, new Address(Register.RSP), new Immediate(operand1.name)));
                break;
            }
            case LOGICAL_OR:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.CMP, left, new Immediate(1)));
                String mergeLabel        = Compiler.generateLabel().name;

                instructions.add(new Instruction(Operation.JE, new Address(mergeLabel, false), null));
                instructions.add(new Instruction(Operation.CMP, right, new Immediate(1)));
                instructions.add(new Instruction(Operation.SETE, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                instructions.add(new Instruction(mergeLabel));
                addTemporary(instructions, tempStack);
                break;
            }
            case LOGICAL_AND:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                String mergeLabel        = Compiler.generateLabel().name;

                instructions.add(new Instruction(Operation.CMP, left, new Immediate(0)));
                instructions.add(new Instruction(Operation.JE, new Address(mergeLabel, false), null));
                instructions.add(new Instruction(Operation.CMP, right, new Immediate(1)));
                instructions.add(new Instruction(Operation.SETE, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                instructions.add(new Instruction(mergeLabel));
                addTemporary(instructions, tempStack);
                break;
            }
            case PARAM:{
                if(operand1.type.isStruct()){
                    this.moveStruct(instructions, symbolTable, tempStack);
                }else {
                    ArgumentSymbol argumentSymbol = (ArgumentSymbol) operand2;
                    if(!argumentSymbol.external){
                        popTemporaryIntoPrimary(instructions, tempStack);
                        instructions.add(new Instruction(getMoveOpFromType(operand1.type), new Address(Register.RSP, true, argumentSymbol.offset), new Address(Register.getPrimaryRegisterFromDataType(operand1.type))));
                    }else{
                        this.addExternalParameter(instructions, tempStack, argumentSymbol);
                    }
                }
                break;
            }
            case CALL:{
                instructions.add(new Instruction(Operation.CALL, new Immediate(operand1.name), null));

                int size = Struct.getFunctionArgumentsStackSize(operand1.name, symbolTable.getFunctions(), symbolTable.getStructs());
                if(size % 16 != 0){
                    size += Compiler.getStackAlignment(size);
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
            case LOAD_POINTER:{
                VariableSymbol variableSymbol = (VariableSymbol) operand1;
                instructions.add(new Instruction(Operation.LEA, new Address(Register.PRIMARY_GENERAL_REGISTER), new Address(Register.RBP, true,variableSymbol.offset)));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case CONVERT:{
                this.convertOperand(instructions,tempStack);
                break;
            }
            case LOAD_MEMBER:{
                ImmediateSymbol memberSymbol    = (ImmediateSymbol) operand2;
                popTemporaryIntoPrimary(instructions, tempStack);
                Operation operation;
                if(result.type.isStruct()){
                    operation = Operation.LEA;
                }else{
                    operation = getMoveOpFromType(result.type);
                }
                instructions.add(new Instruction(operation, new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.RAX, true, Integer.parseInt(memberSymbol.getValue()))));
                addTemporary(instructions, tempStack);
                break;
            }
            case STORE_ARRAY_ITEM:{
                VariableSymbol arraySymbol = (VariableSymbol) operand1;
                ArrayDataType arrayDataType = (ArrayDataType)arraySymbol.type;
                if(arrayDataType.itemType.isStruct()){
                    popIntoRegister(instructions, tempStack, new Address(Register.RSI));
                    String index = ((ImmediateSymbol)operand2).getValue();
                    int offset = arraySymbol.offset + Integer.parseInt(index);
                    instructions.add(new Instruction(Operation.LEA, new Address(Register.RDI), new Address(Register.RBP, true, offset)));

                    int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), operand1.type);
                    instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX), new Immediate(structSize)));
                    instructions.add(new Instruction(Operation.REP, new Address(Register.MOVSB), null));
                    break;
                }

                popTemporaryIntoPrimary(instructions, tempStack);
                Operation moveOp = getMoveOpFromType(arrayDataType.itemType);
                String index = ((ImmediateSymbol)operand2).getValue();
                int offset = arraySymbol.offset + Integer.parseInt(index);
                instructions.add(new Instruction(moveOp, new Address(Register.RBP, true, offset), new Address(Register.getPrimaryRegisterFromDataType(arrayDataType.itemType))));
                break;
            }
            case INDEX:{
                popTemporaryIntoPrimary(instructions, tempStack);
                popTemporaryIntoSecondary(instructions, tempStack);
                Operation operation;
                if(result.type.isStruct()){
                    operation = Operation.LEA;
                }else{
                    operation = getMoveOpFromType(result.type);
                }

                int size = symbolTable.getStructSize(result.type);
                instructions.add(new Instruction(Operation.IMUL, new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate(size)));
                instructions.add(new Instruction(Operation.ADD, new Address(Register.PRIMARY_GENERAL_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                instructions.add(new Instruction(operation, new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.PRIMARY_GENERAL_REGISTER, true)));
                addTemporary(instructions, tempStack);
                break;
            }
            case DEREFERENCE:{
                Address primary = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(getMoveOpFromType(result.type), new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(primary.register, true)));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case REFERENCE_INDEX:{
                popTemporaryIntoPrimary(instructions, tempStack);
                popTemporaryIntoSecondary(instructions, tempStack);

                int size = symbolTable.getStructSize(operand1.type.getTypeFromPointer());
                instructions.add(new Instruction(Operation.IMUL, new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate(size)));
                instructions.add(new Instruction(Operation.ADD, new Address(Register.PRIMARY_GENERAL_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                addTemporary(instructions, tempStack);
                break;
            }
            case JMP_T:{
                popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(getCmpOpFromType(operand1.type), new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate("1")));
                instructions.add(new Instruction(Operation.JE, new Address(operand1.name, false), null));
                break;
            }
            case JMP_F:{
                popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(getCmpOpFromType(operand1.type), new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate("0")));
                instructions.add(new Instruction(Operation.JE, new Address(operand1.name, false), null));
                break;
            }
            case PRE_INC_F:{
                // This is a pointer
                popTemporaryIntoPrimary(instructions, tempStack);

                Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
                Operation moveOp = getMoveOpFromType(result.type);
                instructions.add(new Instruction(moveOp, primary, new Address(Register.RAX, true)));

                instructions.add(new Instruction(Operation.MOV, new Address(Register.SECONDARY_GENERAL_REGISTER), new Immediate("1")));
                Operation convertOp = result.type.isDouble() ? Operation.CVTSI2SD : Operation.CVTSI2SS;
                instructions.add(new Instruction(convertOp, new Address(Register.SECONDARY_SSE_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                Operation addOp = result.type.isDouble() ? Operation.ADDSD : Operation.ADDSS;
                instructions.add(new Instruction(addOp, new Address(Register.PRIMARY_SSE_REGISTER), new Address(Register.SECONDARY_SSE_REGISTER)));

                instructions.add(new Instruction(moveOp, new Address(Register.RAX, true), new Address(Register.PRIMARY_SSE_REGISTER)));
                addTemporary(instructions, tempStack);
                break;
            }
            case PRE_DEC_F:{
                // This is a pointer
                popTemporaryIntoPrimary(instructions, tempStack);

                Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
                Operation moveOp = getMoveOpFromType(result.type);
                instructions.add(new Instruction(moveOp, primary, new Address(Register.RAX, true)));

                instructions.add(new Instruction(Operation.MOV, new Address(Register.SECONDARY_GENERAL_REGISTER), new Immediate("1")));
                Operation convertOp = result.type.isDouble() ? Operation.CVTSI2SD : Operation.CVTSI2SS;
                instructions.add(new Instruction(convertOp, new Address(Register.SECONDARY_SSE_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                Operation addOp = result.type.isDouble() ? Operation.SUBSD : Operation.SUBSS;
                instructions.add(new Instruction(addOp, new Address(Register.PRIMARY_SSE_REGISTER), new Address(Register.SECONDARY_SSE_REGISTER)));

                instructions.add(new Instruction(moveOp, new Address(Register.RAX, true), new Address(Register.PRIMARY_SSE_REGISTER)));
                addTemporary(instructions, tempStack);
                break;

            }
            case POST_INC_F:{
                // This is a pointer
                popTemporaryIntoPrimary(instructions, tempStack);

                Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
                Operation moveOp = getMoveOpFromType(result.type);
                instructions.add(new Instruction(moveOp, primary, new Address(Register.RAX, true)));

                instructions.add(new Instruction(Operation.MOV, new Address(Register.SECONDARY_GENERAL_REGISTER), new Immediate("1")));
                Operation convertOp = result.type.isDouble() ? Operation.CVTSI2SD : Operation.CVTSI2SS;
                instructions.add(new Instruction(convertOp, new Address(Register.SECONDARY_SSE_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                addTemporary(instructions, tempStack);

                Operation addOp = result.type.isDouble() ? Operation.ADDSD : Operation.ADDSS;
                instructions.add(new Instruction(addOp, new Address(Register.PRIMARY_SSE_REGISTER), new Address(Register.SECONDARY_SSE_REGISTER)));

                instructions.add(new Instruction(moveOp, new Address(Register.RAX, true), new Address(Register.PRIMARY_SSE_REGISTER)));
                break;

            }
            case POST_DEC_F:{
                // This is a pointer
                popTemporaryIntoPrimary(instructions, tempStack);

                Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
                Operation moveOp = getMoveOpFromType(result.type);
                instructions.add(new Instruction(moveOp, primary, new Address(Register.RAX, true)));

                instructions.add(new Instruction(Operation.MOV, new Address(Register.SECONDARY_GENERAL_REGISTER), new Immediate("1")));
                Operation convertOp = result.type.isDouble() ? Operation.CVTSI2SD : Operation.CVTSI2SS;
                instructions.add(new Instruction(convertOp, new Address(Register.SECONDARY_SSE_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                addTemporary(instructions, tempStack);

                Operation addOp = result.type.isDouble() ? Operation.SUBSD : Operation.SUBSS;
                instructions.add(new Instruction(addOp, new Address(Register.PRIMARY_SSE_REGISTER), new Address(Register.SECONDARY_SSE_REGISTER)));

                instructions.add(new Instruction(moveOp, new Address(Register.RAX, true), new Address(Register.PRIMARY_SSE_REGISTER)));
                break;

            }
            case PRE_INC_I:{
                this.popTemporaryIntoSecondary(instructions, tempStack);
                // load the actual value
                Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
                instructions.add(new Instruction(Operation.MOV, primary, new Address(Register.RCX, true)));
                // Store the value on the stack
                // Increment the value
                if(operand1.type.isPointer()){
                    instructions.add(new Instruction(Operation.ADD, primary, new Immediate(operand1.type.getTypeFromPointer().getSize())));
                }else{
                    instructions.add(new Instruction(Operation.INC, primary, null));
                }
                // Store the value in the pointer
                instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX, true), primary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case PRE_DEC_I:{
                this.popTemporaryIntoSecondary(instructions, tempStack);
                // load the actual value
                Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
                instructions.add(new Instruction(Operation.MOV, primary, new Address(Register.RCX, true)));
                // Store the value on the stack
                // Increment the value
                if(operand1.type.isPointer()){
                    instructions.add(new Instruction(Operation.SUB, primary, new Immediate(operand1.type.getTypeFromPointer().getSize())));
                }else{
                    instructions.add(new Instruction(Operation.DEC, primary, null));
                }
                // Store the value in the pointer
                instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX, true), primary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case POST_INC_I:{
                this.popTemporaryIntoSecondary(instructions, tempStack);
                // load the actual value
                Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
                instructions.add(new Instruction(Operation.MOV, primary, new Address(Register.RCX, true)));
                // Store the value on the stack
                this.addTemporary(instructions, tempStack);
                // Increment the value
                if(operand1.type.isPointer()){
                    instructions.add(new Instruction(Operation.ADD, primary, new Immediate(operand1.type.getTypeFromPointer().getSize())));
                }else{
                    instructions.add(new Instruction(Operation.INC, primary, null));
                }
                // Store the value in the pointer
                instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX, true), primary));
                break;
            }
            case POST_DEC_I:{
                this.popTemporaryIntoSecondary(instructions, tempStack);
                // load the actual value
                Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
                instructions.add(new Instruction(Operation.MOV, primary, new Address(Register.RCX, true)));
                // Store the value on the stack
                this.addTemporary(instructions, tempStack);
                // Increment the value
                if(operand1.type.isPointer()){
                    instructions.add(new Instruction(Operation.SUB, primary, new Immediate(operand1.type.getTypeFromPointer().getSize())));
                }else{
                    instructions.add(new Instruction(Operation.DEC, primary, null));
                }
                // Store the value in the pointer
                instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX, true), primary));
                break;
            }
            case ADD_I:{
                this.popTemporaryIntoSecondary(instructions, tempStack);
                this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.ADD, new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.getSecondaryRegisterFromDataType(result.type))));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case SUB_I:{
                Address secondary = this.popTemporaryIntoSecondary(instructions, tempStack);
                Address primary = this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.SUB, primary, secondary));
                this.addTemporary(instructions, tempStack);
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
                instructions.add(new Instruction(Operation.IDIV, secondary, null));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case SHL:{
                this.popTemporaryIntoSecondary(instructions, tempStack);
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.SHL, primary, new Address(Register.CL)));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case SHR:{
                this.popTemporaryIntoSecondary(instructions, tempStack);
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.SHR, primary, new Address(Register.CL)));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case ADD_F:{
                Operation operation = result.type.isFloat() ? Operation.ADDSS : Operation.ADDSD;
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(operation, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case SUB_F:{
                Operation operation = result.type.isFloat() ? Operation.SUBSS : Operation.SUBSD;
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(operation, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case MUL_F:{
                Operation operation = result.type.isFloat() ? Operation.MULSS : Operation.MULSD;
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(operation, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case DIV_F:{
                Operation operation = result.type.isFloat() ? Operation.DIVSS : Operation.DIVSD;
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(operation, primary, secondary));
                this.addTemporary(instructions, tempStack);
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
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.AND, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case OR:{
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.OR, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case XOR:{
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.XOR, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case LESS_I:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(getCmpOpFromType(operand1.type), left, right));
                instructions.add(new Instruction(Operation.SETL, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case LESS_EQUAL_I:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(getCmpOpFromType(operand1.type), left, right));
                instructions.add(new Instruction(Operation.SETLE, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case GREATER_I:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(getCmpOpFromType(operand1.type), left, right));
                instructions.add(new Instruction(Operation.SETG, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case GREATER_EQUAL_I:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(getCmpOpFromType(operand1.type), left, right));
                instructions.add(new Instruction(Operation.SETGE, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case EQUAL_I:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(getCmpOpFromType(operand1.type), left, right));
                instructions.add(new Instruction(Operation.SETE, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case NOT_EQUAL_I:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(getCmpOpFromType(operand1.type), left, right));
                instructions.add(new Instruction(Operation.SETNE, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case NOT_I :{
                Address primary = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.NOT, primary, new Immediate("1")));
                break;
            }
            case MOD:{
                instructions.add(new Instruction(Operation.XOR, new Address(Register.RDX), new Address(Register.RDX)));
                popTemporaryIntoPrimary(instructions, tempStack);
                popIntoRegister(instructions,  tempStack, new Address(Register.RCX));
                instructions.add(new Instruction(getMoveOpFromType(result.type), new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.getThirdRegisterFromDataType(result.type))));
                break;
            }
            case LESS_F:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETB, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case LESS_EQUAL_F:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETBE, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case GREATER_F:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETA, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case GREATER_EQUAL_F:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETAE, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case EQUAL_F:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETE, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case NOT_EQUAL_F:{
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETNE, new Address(Register.AL), null));
                instructions.add(new Instruction(Operation.MOVSX, new Address(Register.RAX), new Address(Register.AL)));
                addTemporary(instructions, tempStack);
                break;
            }
            case INC_F:{
                popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.MOV, new Address(Register.SECONDARY_GENERAL_REGISTER), new Immediate("1")));
                boolean doubleOp = operand1.type.isDouble();
                Operation convertOp = doubleOp ? Operation.CVTSI2SD : Operation.CVTSI2SS;
                Operation addOp = doubleOp ? Operation.ADDSD : Operation.ADDSS;
                instructions.add(new Instruction(convertOp, new Address(Register.SECONDARY_SSE_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                instructions.add(new Instruction(addOp, new Address(Register.SECONDARY_SSE_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                addTemporary(instructions, tempStack);
                break;
            }
            case DEC_F:{
                popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.MOV, new Address(Register.SECONDARY_GENERAL_REGISTER), new Immediate("1")));
                boolean doubleOp = operand1.type.isDouble();
                Operation convertOp = doubleOp ? Operation.CVTSI2SD : Operation.CVTSI2SS;
                Operation addOp = doubleOp ? Operation.SUBSD : Operation.SUBSS;
                instructions.add(new Instruction(convertOp, new Address(Register.SECONDARY_SSE_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                instructions.add(new Instruction(addOp, new Address(Register.SECONDARY_SSE_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                addTemporary(instructions, tempStack);
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
            case LOAD_IMM_I:{
                if(result.type.isString()){
                    ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                    Constant constant = constants.get(immediateSymbol.getValue());
                    instructions.add(new Instruction(Operation.MOV, new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate(constant.label())));
                }else{
                    ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                    instructions.add(new Instruction(Operation.MOV, new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate(immediateSymbol.getValue())));
                }
                this.addTemporary(instructions, tempStack);
                break;
            }
            case LOAD_IMM_F:{
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                instructions.add(new Instruction(Operation.MOVSS, new Address(Register.PRIMARY_SSE_REGISTER), new Address(constants.get(immediateSymbol.getValue()).label(), true)));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case LOAD_I, LOAD_F:{
                VariableSymbol variable = (VariableSymbol) operand1;
                if(operand1.type.isStruct()){
                    instructions.add(new Instruction(Operation.LEA, new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.RBP, true, variable.offset)));
                }else{
                    instructions.add(new Instruction(getMoveOpFromType(operand1.type), new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.RBP, true, variable.offset)));
                }
                addTemporary(instructions, tempStack);
                break;
            }
            case LOAD_MEMBER_POINTER:{
                ImmediateSymbol memberSymbol    = (ImmediateSymbol) operand2;
                popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.LEA, new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.PRIMARY_GENERAL_REGISTER, true, Integer.parseInt(memberSymbol.getValue()))));
                addTemporary(instructions, tempStack);
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
}
