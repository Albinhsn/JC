package se.liu.albhe576.project;


public class DotExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s.%s", variable,member.literal);
    }

    public Expr variable;
    public Token member;

    public DotExpr(Expr variable, Token member, int line, String file){
        super(line, file);
        this.variable = variable;
        this.member = member;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException {
        // feels like this can be solved in the parser
        // we don't know whether we're supposed to load this value or not
        variable.compile(symbolTable, quads);
        Symbol lastSymbol = quads.getLastResult();
        Symbol memberSymbol = symbolTable.getMemberSymbol(lastSymbol, this.member.literal);
        quads.createGetField(memberSymbol, lastSymbol);
    }
}
