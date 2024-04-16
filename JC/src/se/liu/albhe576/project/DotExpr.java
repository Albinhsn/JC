package se.liu.albhe576.project;


import java.util.List;
import java.util.Stack;

public class DotExpr implements  Expr{
    @Override
    public String toString() {
        return String.format("%s.%s", variable,member.literal);
    }

    public Expr variable;
    public Token member;

    public DotExpr(Expr variable, Token member){
        this.variable = variable;
        this.member = member;
    }

    @Override
    public List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException {
        // feels like this can be solved in the parser
        // we don't know whether we're supposed to load this value or not
        // The question if whether we need to
        List<Quad> quads = variable.compile(symbolTable);
        Symbol lastSymbol = Quad.getLastOperand1(quads);
        quads.add(new Quad(QuadOp.GET_FIELD, lastSymbol, new ResultSymbol(member.literal), Compiler.generateResultSymbol()));
        return quads;
    }
}
