package se.liu.albhe576.project;

import java.util.List;

public class StructStmt extends Stmt{


    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(String.format("struct %s{\n", this.name));
        for(StructField f: fields){
            s.append(String.format("\t%s\n", f));
        }
        s.append("}\n");
        return s.toString();
    }

    private final String name;
    private final List<StructField> fields;

    public StructStmt(String name, List<StructField> fields){
        this.name = name;
        this.fields = fields;
    }
}
