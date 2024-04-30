package se.liu.albhe576.project;


public class DotExpr extends Expr{
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
        if(!(lastSymbol.type.type == DataTypes.STRUCT && lastSymbol.type.depth <= 2)){
            Compiler.error(String.format("Trying to access member of none struct '%s'", lastSymbol.type.name), line, file);
        }
        if(!symbolTable.isMemberOfStruct(lastSymbol.type, this.member.literal())){
            Compiler.error(String.format("Trying to access member %s of struct %s, doesn't exist!", lastSymbol.type.name, this.member.literal()), line, file);
        }

        Struct struct           = symbolTable.getStructs().get(lastSymbol.type.name);
        int offset = Struct.getFieldOffset(symbolTable.getStructs(), struct, this.member.literal());
        quads.createMember(lastSymbol, new ImmediateSymbol(this.member.literal(),Struct.getMember(struct, this.member.literal()).type(), String.valueOf(offset)));
    }
}
