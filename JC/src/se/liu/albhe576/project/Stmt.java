package se.liu.albhe576.project;

import java.util.List;

public interface Stmt {

    Signature getSignature() throws CompileException;

    BasicBlock compile(final List<Signature> functions, final BasicBlock block, final List<List<Symbol>> symbols) throws CompileException;

}
