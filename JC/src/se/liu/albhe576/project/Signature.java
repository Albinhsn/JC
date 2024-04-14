package se.liu.albhe576.project;

import java.util.List;

public class Signature {

    StructType returnType;
    String name;
    List<StructField> args;

    public Signature(StructType type, String name, List<StructField> args){
        this.returnType =type;
        this.name = name;
        this.args = args;
    }
}
