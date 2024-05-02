package se.liu.albhe576.project;

public class Optimizer {

    private static float floatBinary(float left, float right, QuadOp op) throws CompileException {
        switch(op){
            case ADD_F              ->{return left + right;}
            case SUB_F              ->{return left - right;}
            case DIV_F              ->{return left / right;}
            case MUL_F              ->{return left * right;}
            case LESS_EQUAL_F       -> {return left <= right ? 1 : 0;}
            case LESS_F             -> {return left < right ? 1 : 0;}
            case GREATER_F          -> {return left > right ? 1 : 0;}
            case GREATER_EQUAL_F    -> {return left >= right ? 1 : 0;}
            case EQUAL_F            -> {return left == right ? 1 : 0;}
            case NOT_EQUAL_F        -> {return left != right ? 1 : 0;}
        }
        throw new CompileException(String.format("How could this happen to %s", op.name()));
    }
    private static int intBinary(int left, int right, QuadOp op) throws CompileException {
        switch(op){
            case ADD_I               ->{return left + right;}
            case SUB_I              ->{return left - right;}
            case DIV_I              ->{return left / right;}
            case MUL_I              ->{return left * right;}
            case SHL                ->{return left << right;}
            case SHR                ->{return left >> right;}
            case AND                ->{return left & right;}
            case OR                 ->{return left | right;}
            case XOR                ->{return left ^ right;}
            case LESS_EQUAL_I       -> {return left <= right ? 1 : 0;}
            case LESS_I             -> {return left < right ? 1 : 0;}
            case GREATER_I          -> {return left > right ? 1 : 0;}
            case GREATER_EQUAL_I    -> {return left >= right ? 1 : 0;}
            case EQUAL_I            -> {return left == right ? 1 : 0;}
            case NOT_EQUAL_I        -> {return left != right ? 1 : 0;}
        }
        throw new CompileException(String.format("How could this happen to %s", op.name()));
    }

    public static void optimizeConstantFolding(SymbolTable symbolTable, QuadList leftQuads, QuadList rightQuads, QuadOp op) throws CompileException {
        Quad lQuad = leftQuads.pop();
        Quad rQuad = rightQuads.pop();
        String constant;
        ImmediateSymbol leftImm = (ImmediateSymbol) lQuad.operand1();
        ImmediateSymbol rightImm = (ImmediateSymbol) rQuad.operand1();

        if((lQuad.op() == QuadOp.LOAD_IMM_F || rQuad.op() == QuadOp.LOAD_IMM_F) && op.isBinary()){
            float left = Float.parseFloat(leftImm.getValue());
            float right = Float.parseFloat(rightImm.getValue());
            constant = String.valueOf(floatBinary(left, right, op));

            symbolTable.addConstant(constant, DataTypes.FLOAT);
            leftQuads.createLoadImmediate(symbolTable, new ImmediateSymbol(constant, DataType.getFloat(), constant));
        }else if(lQuad.op() == QuadOp.LOAD_IMM_F || rQuad.op() == QuadOp.LOAD_IMM_F){

            float left = Float.parseFloat(leftImm.getValue());
            float right = Float.parseFloat(rightImm.getValue());
            constant = String.valueOf((int)floatBinary(left, right, op));
            leftQuads.createLoadImmediate(symbolTable, new ImmediateSymbol(constant, DataType.getInt(), constant));

        }else{
            int left = Integer.parseInt(leftImm.getValue());
            int right = Integer.parseInt(rightImm.getValue());
            constant = String.valueOf(intBinary(left, right, op));

            leftQuads.createLoadImmediate(symbolTable, new ImmediateSymbol(constant, DataType.getInt(), constant));
        }
        System.out.printf("%s %s %s -> %s\n", leftImm.getValue(), op, rightImm.getValue(), constant);
    }
    public void optimizeX86Assembly(){}

    public Optimizer(){}
}
