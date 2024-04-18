package se.liu.albhe576.project;


import java.util.List;

public class DotExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s.%s", variable,member.literal);
    }

    public Expr variable;
    public Token member;

    public DotExpr(Expr variable, Token member, int line){
        super(line);
        this.variable = variable;
        this.member = member;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException {
        // feels like this can be solved in the parser
        // we don't know whether we're supposed to load this value or not
        // The question if whether we need to
        QuadList quads = variable.compile(symbolTable);
        Symbol lastSymbol = quads.getLastResult();
        Symbol memberSymbol = symbolTable.getMemberSymbol(lastSymbol, this.member.literal);
        quads.addQuad(QuadOp.GET_FIELD, lastSymbol, memberSymbol, Compiler.generateSymbol(memberSymbol.type));
        return quads;
    }
}
