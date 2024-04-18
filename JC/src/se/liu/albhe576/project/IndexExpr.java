package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class IndexExpr extends Expr{

    @Override
    public String toString() {
        return String.format("%s[%s]", value, index);
    }

    private final Expr value;
    private final Expr index;
    public IndexExpr(Expr value, Expr index, int line){
        super(line);
        this.value = value;
        this.index = index;

    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        // index = 1
        // value = arr
        QuadList val = value.compile(symbolTable);
        Symbol valResult = val.getLastResult();
        Symbol valOperand = val.getLastOperand1();

        QuadList idx = index.compile(symbolTable);
        Symbol idxResult = idx.getLastResult();


        val.addQuad(QuadOp.PUSH, null, null, null);


        val.concat(idx);
        val.addQuad(QuadOp.MOV_REG_CA, idxResult, null, Compiler.generateSymbol(idxResult.type));
        ImmediateSymbol immSymbol = Compiler.generateImmediateSymbol(DataType.getInt(), "8");
        val.addQuad(QuadOp.LOAD_IMM, immSymbol, null, Compiler.generateSymbol(DataType.getInt()));
        val.addQuad(QuadOp.MUL, Compiler.generateSymbol(DataType.getInt()), Compiler.generateSymbol(idxResult.type), Compiler.generateSymbol(DataType.getInt()));
        val.addQuad(QuadOp.MOV_REG_CA, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));
        val.addQuad(QuadOp.POP, null, null, Compiler.generateSymbol(DataType.getInt()));
        val.addQuad(QuadOp.ADD, null, null, Compiler.generateSymbol(DataType.getInt()));
        val.addQuad(QuadOp.INDEX, idxResult, valOperand, Compiler.generateSymbol(valResult.type.getTypeFromPointer()));

        return val;
    }
}
