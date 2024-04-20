package se.liu.albhe576.project;

import java.util.List;
import java.util.Map;

public class Struct {
    public List<StructField> fields;
    public DataType type;
    public String fileName;
    public int getSize(Map<String, Struct> structs){
        int size = 0;
        for(StructField field : fields){
            if(structs.containsKey(field.type.name) && field.type.isStruct()){
                size += structs.get(field.type.name).getSize(structs);
            }else{
                size += 8;
            }

        }
        return size;
    }

    public Struct(String name, List<StructField> fields, String fileName){
        this.fields = fields;
        this.type = DataType.getStruct(name);
        this.fileName = fileName;
    }
}
