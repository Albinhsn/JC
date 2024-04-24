package se.liu.albhe576.project;


public class DotExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s.%s", variable,member.literal());
    }

    private final Expr variable;
    private final Token member;

    public DotExpr(Expr variable, Token member, int line, String file){
        super(line, file);
        this.variable = variable;
        this.member = member;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws  CompileException {
        variable.compile(symbolTable, quads);
        Symbol lastSymbol = quads.getLastResult();

        if(!lastSymbol.type.isStruct() && lastSymbol.type.depth > 1){
            Compiler.error(String.format("Trying to access member of none struct '%s'", lastSymbol.type.name), line, file);
        }
        if(!symbolTable.isMemberOfStruct(lastSymbol.type, this.member.literal())){
            Compiler.error(String.format("Trying to access member %s of struct %s, doesn't exist!", lastSymbol.type.name, this.member.literal()), line, file);
        }

        Symbol memberSymbol = symbolTable.getMemberSymbol(lastSymbol, this.member.literal());
        quads.createGetField(memberSymbol, lastSymbol);
    }
}
