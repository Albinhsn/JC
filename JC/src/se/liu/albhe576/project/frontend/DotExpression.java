package se.liu.albhe576.project.frontend;


import se.liu.albhe576.project.backend.DataType;
import se.liu.albhe576.project.backend.DataTypes;
import se.liu.albhe576.project.backend.SymbolTable;
import se.liu.albhe576.project.backend.Symbol;
import se.liu.albhe576.project.backend.MemberSymbol;
import se.liu.albhe576.project.backend.IntermediateList;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.Compiler;
/**
 * A DotExpression is whenever the user tries to access a member of a struct i.e "foo.bar"
 * @see Structure
 * @see StructureField
 */
public class DotExpression extends Expression
{
    private final Expression variable;
    private final String member;
    public DotExpression(Expression variable, String member, int line, String file){
        super(line, file);
        this.variable = variable;
        this.member = member;
    }

    @Override
    public void compile(SymbolTable symbolTable, IntermediateList intermediates) throws  CompileException {
        variable.compile(symbolTable, intermediates);

        Symbol lastSymbol = intermediates.getLastResult();
        // this is 2 and not 1 since the last load would result in a pointer to the struct and not the struct it self
        // so every pointer to a struct is 1 above its "actual" depth
        // On a semantic level this is 2 since we do "foo." for both pointer and the stack value
        final int maxPointerDepth = 2;
        final DataType lastSymbolType = lastSymbol.getType();
        if(!(lastSymbolType.getType() == DataTypes.STRUCT && lastSymbolType.getDepth()<= maxPointerDepth)){
            Compiler.panic(String.format("Trying to access member of none struct '%s'", lastSymbol.getType().getName()), line, file);
        }

        if(!symbolTable.isMemberOfStruct(lastSymbolType, this.member)){
            Compiler.panic(String.format("Trying to access member %s of struct %s, doesn't exist!", lastSymbolType.getName(), this.member), line, file);
        }

        // Get the offset for the member field and create a member instruction
        Structure structure             = symbolTable.getStructures().get(lastSymbol.getType().getName());
        StructureField member           = Structure.getMember(structure, this.member);
        int offset                      = Structure.getFieldOffset(symbolTable, structure, this.member);
        MemberSymbol memberSymbol       = new MemberSymbol(lastSymbol.getName(), lastSymbol.getType(), offset);

        intermediates.createMember(symbolTable, memberSymbol, member.type());
    }
}
