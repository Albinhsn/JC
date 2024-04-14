package se.liu.albhe576.project;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Compiler {

    private final List<Stmt> stmts;

    public void Compile(String out) throws CompileException, IOException, UnknownSymbolException {

    }

    public Compiler(List<Stmt> stmts){
        this.stmts = stmts;
    }
}
