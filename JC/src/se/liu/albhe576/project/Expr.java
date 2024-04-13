package se.liu.albhe576.project;


import java.util.List;

public class Expr {

    public Value compile(List<Signature> functions, BasicBlock block, List<List<Symbol>> symbols) throws CompileException{
        throw new CompileException("Can't compile empty expr?");
    }

    @Override
    public String toString() {
        return "EMPTY?";
    }

}
