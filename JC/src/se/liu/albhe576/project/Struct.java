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
            if(structs.containsKey(field.type().name) && field.type().isStruct()){
                size += structs.get(field.type().name).getSize(structs);
            }else{
                size += 8;
            }

        }
        return size;
    }

    public Struct(List<StructField> fields, String fileName){
        this.fields = fields;
        this.fileName = fileName;
    }
}
