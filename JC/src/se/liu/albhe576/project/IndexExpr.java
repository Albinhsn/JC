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
        QuadList idx = value.compile(symbolTable);
        Symbol idxOperand = idx.getLastOperand1();
        Symbol idxSymbol = idx.getLastResult();

        Symbol pushedSymbol = Compiler.generateSymbol(idxSymbol.type);
        idx.addQuad(QuadOp.PUSH, idxSymbol, null, pushedSymbol);

        QuadList val = index.compile(symbolTable);
        Symbol valSymbol = val.getLastResult();

        idx.concat(val);
        int structSize = symbolTable.getStructSize(idxOperand.type.name);
        idx.addQuad(QuadOp.MOV_REG_CA, null, null, null);
        idx.addQuad(QuadOp.LOAD_IMM, Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(structSize)), null, Compiler.generateSymbol(DataType.getInt()));
        idx.addQuad(QuadOp.MUL, null, null, Compiler.generateSymbol(DataType.getInt()));
        idx.addQuad(QuadOp.MOV_REG_CA, null, null, null);


        Symbol poppedSymbol = Compiler.generateSymbol(pushedSymbol.type);
        idx.addQuad(QuadOp.POP, pushedSymbol, null, poppedSymbol);
        idx.addQuad(QuadOp.INDEX, idxOperand, valSymbol, Compiler.generateSymbol(idxSymbol.type.getTypeFromPointer()));
        return idx;
    }
}
