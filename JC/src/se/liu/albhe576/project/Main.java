package se.liu.albhe576.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, CompileException {
        if(args.length == 0){
            System.out.println("Need filename!");
            return;
        }
        String filePath = args[0];
        String s = Files.readString(Path.of(filePath));

        List<String> included = new ArrayList<>();
        included.add(filePath);
        Parser parser = new Parser(new Scanner(s), included, new HashMap<>(), filePath);
        Compiler compiler = new Compiler(parser.getStructs(), parser.parse(), parser.getExtern());
        compiler.Compile("out.asm");
    }
}
