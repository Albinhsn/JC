package se.liu.albhe576.project;

import java.util.List;
import java.util.Stack;

public interface Stmt {

    List<Quad> compile(Stack<List<Symbol>> symbolTable) throws UnknownSymbolException, CompileException;
}
