package se.liu.albhe576.project.backend;

/**
 * The main type of exception for when the compiler found something it shouldn't.
 * This should only be cast when the compiler internally fails and not when the user does so, if so use panic method in Compiler
 * @see Compiler
 */
public class CompileException extends Exception{
    public CompileException(String msg){
        super(msg);
    }
}
