package se.liu.albhe576.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException, CompileException, UnknownSymbolException {
        String s = Files.readString(Path.of("resources/test.jc"));
        System.out.println(s);
        Scanner scanner = new Scanner(s);
        Parser parser = new Parser(scanner);
        Compiler compiler = new Compiler(parser.parse());
        compiler.Compile("out.jc");
    }
}
