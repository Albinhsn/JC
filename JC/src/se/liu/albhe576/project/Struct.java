package se.liu.albhe576.project;

import java.util.List;
import java.util.Map;

public class Struct {
    private final List<StructField> fields;
    private final String fileName;

    public List<StructField> getFields(){
       return this.fields;
    }
    public String getFilename(){
        return this.fileName;
    }
    public int getSize(Map<String, Struct> structs){
        int size = 0;
        for(StructField field : fields){
            size += SymbolTable.getStructSize(structs, field.type());
        }
        return size;
    }
    public static int getFieldOffset(Map<String, Struct> structs, Struct struct, String memberName)throws CompileException  {
        int size = 0;
        for(StructField field : struct.getFields()){
            if(field.name().equals(memberName)){
                return size;
            }
            size += SymbolTable.getStructSize(structs, field.type());
        }
        throw new CompileException(String.format("Couldn't find member %s?\n", memberName));
    }
    public static int getFunctionArgumentsStackSize(String name, Map<String, Function> functions, Map<String, Struct> structs) {
        Function function = functions.get(name);
        if (function.external) {
            return 0;
        }

        int argSize = 0;
        if (function.getArguments() != null) {
            for (StructField field : function.getArguments()) {
                argSize += SymbolTable.getStructSize(structs, field.type());
            }
        }
        return argSize;
    }

    public Struct(List<StructField> fields, String fileName){
        this.fields = fields;
        this.fileName = fileName;
    }
}
