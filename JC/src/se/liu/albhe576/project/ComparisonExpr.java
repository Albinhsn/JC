package se.liu.albhe576.project;

import java.util.HashMap;
import java.util.Map;

public class ComparisonExpr extends Expr {
    public Expr left;
    public Expr right;
    public Token op;
    public ComparisonExpr(Expr left, Expr right, Token op, int line, String file){
        super(line,file);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    private static QuadOp convertOpToFloat(QuadOp op) {
        switch(op){
            case SETLE -> {return QuadOp.SETBE ;}
            case SETL -> {return QuadOp.SETB;}
            case SETG -> {return QuadOp.SETA;}
            case SETGE -> {return QuadOp.SETAE;}
            default -> {return op;}
        }
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads)  throws CompileException{
        left.compile(symbolTable, quads);
        Symbol lResult = quads.getLastResult();
        DataType lType = lResult.type;

        QuadList rQuads = new QuadList();
        right.compile(symbolTable, rQuads);
        Symbol rResult = rQuads.getLastResult();
        DataType rType = rResult.type;

        QuadOp op = QuadOp.fromToken(this.op);
        if(lType.isFloatingPoint() || rType.isFloatingPoint()){
            op = convertOpToFloat(op);
        }

        if(lType.isFloatingPoint() && !rType.isFloatingPoint()){
            rResult = rQuads.createConvertIntToFloat(rResult);
        }else if(!lType.isFloatingPoint() && rType.isFloatingPoint()){
            lResult = quads.createConvertIntToFloat(lResult);
        }

        quads.createSetupBinary(rQuads, lResult, rResult);
        quads.createCmp(lResult, rResult);
        quads.addQuad(op, null, null, Compiler.generateSymbol(DataType.getInt()));
    }
}
