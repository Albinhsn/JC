package se.liu.albhe576.project;

import java.util.List;

public class Quad {

    @Override
    public String toString() {
        return op.name() + "\t\t" +
                operand1 + "\t\t" +
                operand2 + "\t\t" +
                result;
    }

    public QuadOp op;
    public Symbol operand1;
    public Symbol operand2;
    public Symbol result;

    public static Symbol getLastResult(List<Quad> quads){
        return quads.get(quads.size() - 1).result;
    }
    public static Symbol getLastOperand1(List<Quad> quads){
        return quads.get(quads.size() - 1).operand1;
    }

    public static Quad insertLabel(ResultSymbol label){
        return new Quad(QuadOp.LABEL, label, null, null);
    }

    public Quad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.result   = result;

    }


    public static void insertBooleanComparison(List<Quad> quads, String immediateLiteral){
        ImmediateSymbol immediateSymbol = new ImmediateSymbol(new Token(TokenType.TOKEN_INT_LITERAL, 0, immediateLiteral));
        ResultSymbol immLoadResult = Compiler.generateResultSymbol();
        quads.add(new Quad(QuadOp.PUSH, null, null, null));
        quads.add(new Quad(QuadOp.LOAD_IMM, immediateSymbol, null, immLoadResult));
        quads.add(new Quad(QuadOp.MOV_REG_CA, null, null, null));
        quads.add(new Quad(QuadOp.POP, null, null, null));
        quads.add(new Quad(QuadOp.CMP, null, null,null));
    }

    public static void insertJMPOnComparisonCheck(List<Quad> quads, ResultSymbol jmpLocation, boolean jumpIfTrue){
        String immLiteral;
        if(jumpIfTrue){
            immLiteral = "1";
        }else{
            immLiteral = "0";
        }
        insertBooleanComparison(quads, immLiteral);
        quads.add(new Quad(QuadOp.JE, jmpLocation, null, null));

    }
    public String emit(Stack stack, QuadOp prevOp, List<FunctionSymbol> functions, List<StructSymbol> structSymbols) throws UnknownSymbolException {
        switch(this.op){
            case LOAD_IMM -> {
                ImmediateSymbol imm = (ImmediateSymbol) operand1;
                if(prevOp == QuadOp.LOAD_IMM || prevOp == QuadOp.LOAD){
                    return String.format("mov rcx, %s", imm.value.literal);
                }
                return String.format("mov rax, %s", imm.value.literal);
            }
            case INC -> {
                return "inc rax";
            }
            case DEC -> {
                return "dec rax";
            }
            case ADD -> {
                return "add rax, rcx";
            }
            case SUB -> {
                return "sub rax, rcx";
            }
            case MUL -> {
                return "mul rcx";
            }
            case DIV -> {
                return "div rcx";
            }
            case LOAD_POINTER ->{
                return stack.loadStructPointer(operand1.name);
            }
            case LOAD ->{
                return stack.loadVariable(operand1.name, prevOp);
            }
            case SET_FIELD -> {
                return stack.storeField(result.name, operand2.name);
            }
            case GET_FIELD -> {
                return stack.loadField(operand1.name, operand2.name);
            }
            case STORE -> {
                VariableSymbol variableSymbol = (VariableSymbol) result;
                return stack.storeVariable(variableSymbol.type, result.name);
            }
            case CMP -> {
                return "cmp rax, rcx";
            }
            case JMP ->{
                return String.format("jmp %s", operand1.name);
            }
            case JNZ ->{
                return String.format("jnz %s", operand1.name);
            }
            case JE->{
                return String.format("je %s", operand1.name);
            }
            case LABEL ->{
                return operand1.name + ":";
            }
            case SETE -> {
                return "sete al\nmovzx rax, al";
            }
            case SETLE -> {
                return "setle al\nmovzx rax, al";
            }
            case SETG -> {
                return "setg al\nmovzx rax, al";
            }
            case SETGE -> {
                return "setge al\nmovzx rax, al";
            }
            case SETL -> {
                return "setl al\nmovzx rax, al";
            }
            case PUSH ->{
                return "push rax";
            }
            case POP ->{
                return "pop rax";
            }
            case MOV_REG_CA ->{
                return "mov rcx, rax";
            }
            case MOV_REG_AC ->{
                return "mov rax, rcx";
            }
            case CALL ->{
                int argSize = 8;
                for(FunctionSymbol symbol : functions){
                    if(symbol.name.equals(operand1.name)){
                        argSize *= symbol.arguments.size();
                        break;
                    }
                }
                if(argSize != 0){
                    return String.format("call %s\nadd rsp, %d", operand1.name, argSize);
                }
                return String.format("call %s", operand1.name);
            }
            case LOGICAL_NOT ->{
                return "xor rax, 1";
            }
            case NOT ->{
                return "not rax\ninc rax";
            }
            case RET ->{
                int localVariables = stack.getLocalSize();
                if(localVariables > 0){
                    return String.format("mov rsp, rbp\npop rbp\nret", localVariables);
                }
                return "mov rsp, rbp\npop rbp\nret";
            }
        }
        throw new UnknownSymbolException(String.format("Don't know how to do %s", op));
    }
}
