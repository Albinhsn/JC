package se.liu.albhe576.project.frontend;

import se.liu.albhe576.project.backend.CodeGenerator;
import se.liu.albhe576.project.backend.CompileException;
import se.liu.albhe576.project.backend.IntelCodeGenerator;
import se.liu.albhe576.project.backend.Linux64BitCallingConvention;
import se.liu.albhe576.project.backend.SymbolTable;
import se.liu.albhe576.project.backend.Compiler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * The main class where it all begins
 * When ran it needs an argument for the file to use
 * @see Compiler
 * @see Parser
 * @see Scanner
 */
public class Main {

    public static void main(String[] args) {
        if(args.length == 0){
            System.out.println("Need filename!");
            return;
        }
        FileHandler fileHandler;
        Logger mainLogger = Logger.getLogger("main");
        try{
            fileHandler = new FileHandler("log.log");
        }catch (IOException e){
            mainLogger.severe(e.getMessage());
            System.out.println("Couldn't create filehandler");
            return;
        }

        mainLogger.addHandler(fileHandler);
        String filePath = args[0];
        String s;
        try{
            s = Files.readString(Path.of(filePath));
        }catch (IOException e){
            mainLogger.severe(String.format("Failed to read file from '%s'\n%s", filePath, e.getMessage()));
            return;
        }


        List<String> included = new ArrayList<>();
        included.add(filePath);
        Parser parser = new Parser(new Scanner(s, filePath), included, filePath, fileHandler);
        try{
            parser.parse();
            SymbolTable symbolTable     = new SymbolTable(parser.getStructures(), parser.getFunctions());

            CodeGenerator codeGenerator = new IntelCodeGenerator<>(symbolTable, new Linux64BitCallingConvention());
            Compiler compiler           = new Compiler(symbolTable, codeGenerator, fileHandler);
            compiler.compile("out.asm");
        }catch (CompileException | IOException e){
            mainLogger.severe(e.getMessage());
            System.out.printf("Failed to compiled due to:\n\"%s\"\npls send bug report with sample code :)", e.getMessage());
        }
    }
}
