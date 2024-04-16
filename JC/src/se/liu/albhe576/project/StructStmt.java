package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class StructStmt implements Stmt{
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(String.format("struct %s{\n", this.name));
        for(StructField f: fields){
            s.append(String.format("\t%s;\n", f));
        }
        s.append("}\n");
        return s.toString();
    }

    private final Token name;
    private final List<StructField> fields;

    public StructStmt(Token name, List<StructField> fields){
        this.name = name;
        this.fields = fields;
    }

    @Override
    public List<Quad> compile(SymbolTable symbolTable) {
        symbolTable.structs.add(new Struct(name.literal, fields));
        return new ArrayList<>();
    }
}
