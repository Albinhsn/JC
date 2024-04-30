package se.liu.albhe576.project;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result) {
    @Override
    public String toString() {
        return String.format("%s %s %s %s",op.name(),operand1,operand2,result);
    }

    public List<Instruction> convertOperand( TemporaryVariableStack tempStack) throws CompileException {
        List<Instruction> convertInstructions = new ArrayList<>();
        if(operand1.type.isFloat() && result.type.isInt()){
            VariableSymbol value = tempStack.popVariable();
            Address primary =new Address(Register.getPrimaryRegisterFromDataType(value.type));
            convertInstructions.add(new Instruction(Operation.MOVSS, primary, new Address(Register.RBP, true, value.offset)));
            convertInstructions.add(new Instruction(Operation.CVTSS2SI,new Address(Register.PRIMARY_GENERAL_REGISTER), new Address(Register.PRIMARY_SSE_REGISTER)));
            int offset = tempStack.addVariable(result.name, result.type);
            convertInstructions.add(new Instruction(Operation.MOV, new Address(Register.RBP, true, offset), new Address(Register.PRIMARY_GENERAL_REGISTER)));
        }
        return convertInstructions;
    }

    private List<Instruction> moveStruct(SymbolTable symbolTable,TemporaryVariableStack tempStack){
        // this.add(new Quad(op, symbol, null, loaded));
        List<Instruction> instructions = new ArrayList<>();


        return instructions;
    }
    private static final Instruction[] EPILOGUE_INSTRUCTIONS = new Instruction[]{
            new Instruction(Operation.MOV, new Address(Register.RSP), new Address(Register.RBP)),
            new Instruction(Operation.POP, new Address(Register.RBP), null)
    };
    private Operation getMoveOpFromType(DataType type){
        if(!type.isFloatingPoint()){
            return Operation.MOV;
        }else if(type.isFloat()) {
            return Operation.MOVSS;
        }
        return Operation.MOVSD;
    }

    private Address popIntoRegister(List<Instruction> instructions, VariableSymbol value, Address primary){
        Operation moveOp = getMoveOpFromType(value.type);
        instructions.add(new Instruction(moveOp, primary, new Address(Register.RBP, true, value.offset)));
        return primary;

    }
    private Address popTemporaryIntoSecondary(List<Instruction> instructions, TemporaryVariableStack tempStack){
        VariableSymbol value            = tempStack.popVariable();
        Address primary                 = new Address(Register.getSecondaryRegisterFromDataType(value.type));
        return popIntoRegister(instructions, value, new Address(primary.register));
    }
    private Address popTemporaryIntoPrimary(List<Instruction> instructions, TemporaryVariableStack tempStack){
        VariableSymbol value            = tempStack.popVariable();
        Address primary                 = new Address(Register.getPrimaryRegisterFromDataType(value.type));
        return popIntoRegister(instructions, value, new Address(primary.register));
    }
    private void addTemporary(List<Instruction> instructions, TemporaryVariableStack tempStack) throws CompileException {
        int offset;
        if(result.type.isStruct()){
            offset = tempStack.addVariable(result.name, DataType.getPointerFromType(result.type));
        }else{
            offset = tempStack.addVariable(result.name, result.type);
        }
        Operation moveOp = getMoveOpFromType(result.type);
        instructions.add(new Instruction(moveOp, new Address(Register.RBP, true, offset), new Address(Register.getPrimaryRegisterFromDataType(result.type))));
    }

    public List<Instruction> emitInstructions(SymbolTable symbolTable, Map<String, Constant> constants,TemporaryVariableStack tempStack) throws CompileException{
        List<Instruction> instructions = new ArrayList<>();
        switch(op){
            case ALLOCATE:{
                instructions.add(new Instruction(Operation.SUB, new Address(Register.RSP), new Immediate(operand1.name)));
                break;
            }
            case LOGICAL_AND:{
                break;
            }
            case LOGICAL_OR:{
                break;
            }
            case PARAM:{
                // Kinda need to create a symbol for this
                // Needs to include the type of function and which number of arg it is (at least if external)
                if(operand1.type.isStruct()){
                    instructions.addAll(this.moveStruct(symbolTable, tempStack));
                }else if(!operand1.type.isFloatingPoint()){
                    instructions.add(new Instruction(Operation.MOV, new Address(Register.RBP, true, Integer.parseInt(operand2.name)),new Address(Register.getPrimaryRegisterFromDataType(operand1.type))));
                }else if(operand1.type.isFloat()){
                    instructions.add(new Instruction(Operation.MOVSS, new Address(Register.RBP, true, Integer.parseInt(operand2.name)), new Address(Register.PRIMARY_SSE_REGISTER)));
                }else{
                    instructions.add(new Instruction(Operation.MOVSD, new Address(Register.RBP, true, Integer.parseInt(operand2.name)), new Address(Register.PRIMARY_SSE_REGISTER)));
                }
                break;
            }
            case CALL:{
                instructions.add(new Instruction(Operation.CALL, new Immediate(operand1.name), null));

                int size = Struct.getFunctionArgumentsStackSize(operand1.name, symbolTable.getFunctions(), symbolTable.getStructs());
                size += Compiler.getStackAlignment(size);
                instructions.add(new Instruction(Operation.ADD, new Address(Register.RSP), new Immediate(size)));
                break;
            }
            case JMP :{
                instructions.add(new Instruction(Operation.JMP, new Immediate(operand1.name), null));
                break;
            }
            case LOAD_POINTER :{
                VariableSymbol variableSymbol = (VariableSymbol) operand1;
                instructions.add(new Instruction(Operation.LEA, new Address(Register.PRIMARY_GENERAL_REGISTER), new Address(Register.RBP, true,variableSymbol.offset)));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case CONVERT:{
                instructions.addAll(this.convertOperand(tempStack));
                break;
            }
            case LOAD_MEMBER:{
                ImmediateSymbol memberSymbol    = (ImmediateSymbol) operand2;
                popTemporaryIntoPrimary(instructions, tempStack);
                Operation operation             = getMoveOpFromType(result.type);
                instructions.add(new Instruction(operation, new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.RBP, true, Integer.parseInt(memberSymbol.getValue()))));
                addTemporary(instructions, tempStack);
                break;
            }
            case STORE_ARRAY_ITEM:{
                VariableSymbol arraySymbol = (VariableSymbol) operand1;
                Address primary = popTemporaryIntoPrimary(instructions, tempStack);
                Operation moveOp = getMoveOpFromType(arraySymbol.type);
                String index = ((ImmediateSymbol)operand2).getValue();
                int offset = arraySymbol.offset + Integer.parseInt(index);
                instructions.add(new Instruction(moveOp, new Address(Register.RBP, true, offset), primary));
                break;
            }
            case INDEX:{
                popTemporaryIntoPrimary(instructions, tempStack);
                popTemporaryIntoSecondary(instructions, tempStack);
                Operation operation             = getMoveOpFromType(result.type);

                int size = symbolTable.getStructSize(operand2.type);
                instructions.add(new Instruction(Operation.IMUL, new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate(size)));
                instructions.add(new Instruction(Operation.ADD, new Address(Register.PRIMARY_GENERAL_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                instructions.add(new Instruction(operation, new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.PRIMARY_GENERAL_REGISTER, true)));
                addTemporary(instructions, tempStack);
                break;
            }
            case DEREFERENCE:{
                Address primary = popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.MOV, new Address(Register.PRIMARY_GENERAL_REGISTER), new Address(primary.register, true)));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case REFERENCE_INDEX:{
                popTemporaryIntoPrimary(instructions, tempStack);
                popTemporaryIntoSecondary(instructions, tempStack);

                int size = symbolTable.getStructSize(operand2.type);
                instructions.add(new Instruction(Operation.IMUL, new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate(size)));
                instructions.add(new Instruction(Operation.ADD, new Address(Register.PRIMARY_GENERAL_REGISTER), new Address(Register.SECONDARY_GENERAL_REGISTER)));
                addTemporary(instructions, tempStack);
                break;
            }
            case JMP_T:{
                popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.CMP, new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate("1")));
                instructions.add(new Instruction(Operation.JE, new Address(operand1.name, false), null));
                break;
            }
            case JMP_F:{
                popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.CMP, new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate("0")));
                instructions.add(new Instruction(Operation.JE, new Address(operand1.name, false), null));
                break;
            }
            case PRE_INC_I:{
                this.popTemporaryIntoSecondary(instructions, tempStack);
                // load the actual value
                Address primary = new Address(Register.getPrimaryRegisterFromDataType(operand1.type));
                instructions.add(new Instruction(Operation.MOV, primary, new Address(Register.RCX, true)));
                // Store the value on the stack
                // Increment the value
                instructions.add(new Instruction(Operation.INC, primary, null));
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
                instructions.add(new Instruction(Operation.DEC, primary, null));
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
                instructions.add(new Instruction(Operation.INC, primary, null));
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
                instructions.add(new Instruction(Operation.DEC, primary, null));
                // Store the value in the pointer
                instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX, true), primary));
                break;
            }
            case ADD_I:{
                Address primary = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.ADD, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case SUB_I:{
                Address primary = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.SUB, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case MUL_I:{
                Address primary = this.popTemporaryIntoPrimary(instructions, tempStack);
                this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.MUL, primary, null));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case DIV_I:{
                Address primary = this.popTemporaryIntoPrimary(instructions, tempStack);
                this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.IDIV, primary, null));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case SHL:{
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.SHL, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case SHR:{
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.SHR, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case ADD_F:{
                Operation operation = result.type.isFloat() ? Operation.ADDSS : Operation.ADDSD;
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(operation, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case SUB_F:{
                Operation operation = result.type.isFloat() ? Operation.SUBSS : Operation.SUBSD;
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(operation, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case MUL_F:{
                Operation operation = result.type.isFloat() ? Operation.MULSS : Operation.MULSD;
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(operation, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case DIV_F:{
                Operation operation = result.type.isFloat() ? Operation.DIVSS : Operation.DIVSD;
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
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
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.AND, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case OR:{
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.OR, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case XOR:{
                Address primary     = this.popTemporaryIntoPrimary(instructions, tempStack);
                Address secondary   = this.popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.XOR, primary, secondary));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case LESS_I:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.CMP, left, right));
                instructions.add(new Instruction(Operation.SETL, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case LESS_EQUAL_I:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.CMP, left, right));
                instructions.add(new Instruction(Operation.SETLE, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case GREATER_I:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.CMP, left, right));
                instructions.add(new Instruction(Operation.SETG, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case GREATER_EQUAL_I:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.CMP, left, right));
                instructions.add(new Instruction(Operation.SETGE, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case EQUAL_I:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.CMP, left, right));
                instructions.add(new Instruction(Operation.SETE, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case NOT_EQUAL_I:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                instructions.add(new Instruction(Operation.CMP, left, right));
                instructions.add(new Instruction(Operation.SETNE, new Address(Register.AL), null));
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
                VariableSymbol value            = tempStack.popVariable();
                popIntoRegister(instructions,  value, new Address(Register.RCX));
                instructions.add(new Instruction(getMoveOpFromType(result.type), new Address(Register.getPrimaryRegisterFromDataType(result.type)), new Address(Register.getThirdRegisterFromDataType(result.type))));
                break;
            }
            case LESS_F:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETB, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case LESS_EQUAL_F:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETBE, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case GREATER_F:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETA, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case GREATER_EQUAL_F:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETAE, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case EQUAL_F:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETE, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case NOT_EQUAL_F:{
                Address left = popTemporaryIntoPrimary(instructions, tempStack);
                Address right = popTemporaryIntoSecondary(instructions, tempStack);
                Operation op = operand1.type.isFloat() ? Operation.COMISS : Operation.COMISD;
                instructions.add(new Instruction(op, left, right));
                instructions.add(new Instruction(Operation.SETNE, new Address(Register.AL), null));
                addTemporary(instructions, tempStack);
                break;
            }
            case INC_F:{
                popTemporaryIntoPrimary(instructions, tempStack);
                instructions.add(new Instruction(Operation.MOV, new Address(Register.SECONDARY_GENERAL_REGISTER), new Immediate("1.0")));
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
                instructions.add(new Instruction(Operation.MOV, new Address(Register.SECONDARY_GENERAL_REGISTER), new Immediate("1.0")));
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
                this.popTemporaryIntoPrimary(instructions, tempStack);
                instructions.addAll(List.of(EPILOGUE_INSTRUCTIONS));
                instructions.add(new Instruction(Operation.RET, null, null));
                break;
            }
            case LOAD_IMM_I:{
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                instructions.add(new Instruction(Operation.MOV, new Address(Register.PRIMARY_GENERAL_REGISTER), new Immediate(immediateSymbol.getValue())));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case LOAD_IMM_F:{
                ImmediateSymbol immediateSymbol = (ImmediateSymbol) operand1;
                instructions.add(new Instruction(Operation.MOVSS, new Address(Register.PRIMARY_SSE_REGISTER), new Address(constants.get(immediateSymbol.getValue()).label(), true)));
                this.addTemporary(instructions, tempStack);
                break;
            }
            case LOAD_I:{
                VariableSymbol variable = (VariableSymbol) operand1;
                instructions.add(new Instruction(Operation.LEA, new Address(Register.RAX), new Address(Register.RBP, true, variable.offset)));
                addTemporary(instructions, tempStack);
                int offset = tempStack.addVariable(result.name, DataType.getPointerFromType(result.type));
                instructions.add(new Instruction(Operation.MOV, new Address(Register.RBP, true, offset), new Address(Register.RAX)));
                break;
            }
            case STORE:{
                VariableSymbol value    = tempStack.popVariable();
                if(!(result instanceof VariableSymbol)){
                    System.out.println("");
                }
                VariableSymbol variable = (VariableSymbol) result;
                instructions.add(new Instruction(Operation.MOV, new Address(Register.getPrimaryRegisterFromDataType(value.type)), new Address(Register.RBP, true, value.offset)));
                instructions.add(new Instruction(Operation.MOV, new Address(Register.RBP, true, variable.offset), new Address(Register.getPrimaryRegisterFromDataType(operand1.type))));
                break;
            }
            case ASSIGN:{
                VariableSymbol pointer  = tempStack.popVariable();
                VariableSymbol value    = tempStack.popVariable();
                instructions.add(new Instruction(Operation.MOV, new Address(Register.getPrimaryRegisterFromDataType(value.type)), new Address(Register.RBP, true, value.offset)));
                instructions.add(new Instruction(Operation.MOV, new Address(Register.getSecondaryRegisterFromDataType(pointer.type)), new Address(Register.RBP, true, pointer.offset)));
                instructions.add(new Instruction(Operation.MOV, new Address(Register.RCX, true), new Address(Register.getPrimaryRegisterFromDataType(result.type))));
                break;
            }
            case LOAD_F:{
                VariableSymbol variable = (VariableSymbol) operand1;
                Address primary = new Address(Register.XMM0);
                Operation movOp = variable.type.isFloat() ? Operation.MOVSS : Operation.MOVSD;
                instructions.add(new Instruction(movOp, primary, new Address(Register.RBP, true, variable.offset)));
                this.addTemporary(instructions, tempStack);
                break;
            }
            default : {throw new CompileException(String.format("Don't know how to make instructions from %s", op.name()));}
        }
        return instructions;
    }
}
