package se.liu.albhe576.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException, IllegalCharacterException, UnterminatedStringException {
        String s = Files.readString(Path.of("resources/test.jc"));
        Scanner scanner = new Scanner(s);
        Parser parser = new Parser(scanner);
        for(Stmt stmt : parser.parse()){
            System.out.println(stmt);
        }
    }
}
