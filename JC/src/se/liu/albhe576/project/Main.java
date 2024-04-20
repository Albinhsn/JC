package se.liu.albhe576.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException, CompileException {
        String filePath = "resources/main.jc";
        String s = Files.readString(Path.of(filePath));
        System.out.println(s);
        Scanner scanner = new Scanner(s);
        List<String> included = new ArrayList<>();
        Map<String, Struct> structs = new HashMap<>();
        included.add(filePath);
        Parser parser = new Parser(scanner, included, structs, filePath);
        Compiler compiler = new Compiler(parser.getStructs(), parser.parse(), parser.getExtern());
        compiler.Compile("out.asm");
    }
}
