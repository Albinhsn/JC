package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public class IndexExpr implements Expr{

    @Override
    public String toString() {
        return String.format("%s[%s]", value, index);
    }

    private final Expr value;
    private final Expr index;
    public IndexExpr(Expr value, Expr index){
        this.value = value;
        this.index = index;

    }

    @Override
    public List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        List<Quad> idx = value.compile(symbolTable);
        Symbol idxOperand = Quad.getLastOperand1(idx);
        Symbol idxSymbol = Quad.getLastResult(idx);
        idx.add(new Quad(QuadOp.PUSH, null, null, null));

        List<Quad> val = index.compile(symbolTable);
        Symbol valSymbol = Quad.getLastResult(val);

        idx.addAll(val);
        idx.add(new Quad(QuadOp.MOV_REG_CA, null, null, null));
        idx.add(new Quad(QuadOp.POP, null, null, null));
        idx.add(new Quad(QuadOp.INDEX, idxOperand, valSymbol, Compiler.generateSymbol(DataType.getTypeFromPointer(idxSymbol.type))));
        return idx;
    }
}
