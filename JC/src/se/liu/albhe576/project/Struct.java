package se.liu.albhe576.project;

import java.util.List;

public class Struct {
    public List<StructField> fields;

    public DataType type;
    public int getSize(){
        return fields.size() * 8;
    }

    public Struct(String name, List<StructField> fields){
        this.fields = fields;
        this.type = DataType.getStruct(name);

    }
}
