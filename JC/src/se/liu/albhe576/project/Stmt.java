package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public interface Stmt {

    List<Quad> compile(SymbolTable symbolTable) throws UnknownSymbolException, CompileException, InvalidOperation, UnexpectedTokenException;
}
