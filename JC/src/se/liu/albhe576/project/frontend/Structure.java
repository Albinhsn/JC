package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.*;
import java.util.List;
import java.util.Map;

/**
 * Defines what is a user defined structure within the language
 * Is just a block of memory with fields associated the the memory within it
 * @see StructureField
 */
public class Structure
{
    private final List<StructureField> fields;
    private final String fileName;

    public List<StructureField> getFields(){
       return this.fields;
    }
    public String getFilename(){
        return this.fileName;
    }
    public int getSize(Map<String, Structure> structures){
        int size = 0;
        for(StructureField field : fields){
            size += SymbolTable.getStructureSize(structures, field.type());
        }
        return size;
    }
    public static int getFieldOffset(SymbolTable symbolTable, Structure structure, String memberName)throws CompileException  {
        int size = 0;
        for(StructureField field : structure.getFields()){
            if(field.name().equals(memberName)){
                return size;
            }
            size += symbolTable.getStructureSize(field.type());
        }
        throw new CompileException(String.format("Couldn't find member %s?\n", memberName));
    }
    public static StructureField getMember(Structure structure, String memberName)throws CompileException  {
        for(StructureField field : structure.fields){
            if(field.name().equals(memberName)){
                return field;
            }
        }
        throw new CompileException(String.format("Couldn't find member %s?\n", memberName));
    }
    public Structure(List<StructureField> fields, String fileName){
        this.fields = fields;
        this.fileName = fileName;
    }
}
