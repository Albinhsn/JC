package se.liu.albhe576.project;

import java.util.List;

public class StructSymbol extends  Symbol {
    List<StructField> fields;

    public StructSymbol(String name, List<StructField> fields){
        super(name);
        this.fields = fields;

    }
}
