package se.liu.albhe576.project;


import java.util.List;
import java.util.Stack;

public interface Expr {

    List<Quad> compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, InvalidOperation, UnexpectedTokenException;
}
