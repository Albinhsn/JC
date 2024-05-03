package se.liu.albhe576.project;

import java.util.List;
import java.util.Map;

public class ExternalFunction extends Function{
    public ExternalFunction(List<StructField> arguments, DataType returnType, List<Stmt> stmts, String file, int line) {
        super(arguments, returnType, stmts, file, line);
    }

    @Override
    public int call(SymbolTable symbolTable, QuadList argumentQuads, List<Expr> args) throws CompileException {
        int floatCount = 0, generalCount = 0;
        final int maxFloatCount     = 8;
        final int maxGeneralCount   = 6;
        int argumentStackSize       = 0;
        final int rcxLocation       = 4;

        QuadList floatQuads = new QuadList();
        QuadList generalQuads = new QuadList();
        for (Expr argument : args) {
            QuadList currentArgument = new QuadList();

            argument.compile(symbolTable, currentArgument);
            Symbol result    = currentArgument.getLastResult();

            if (result.type.isFloatingPoint()) {
                if (result.type.isFloat()) {
                    currentArgument.createConvert(symbolTable, result, DataType.getDouble());
                }
                currentArgument.createParam(currentArgument.getLastResult(), argumentStackSize, floatCount, true);
                if (floatCount >= maxFloatCount) {
                    argumentStackSize += 8;
                }
                floatCount++;

                currentArgument.addAll(floatQuads);
                floatQuads = currentArgument;

            } else {
                currentArgument.createParam(currentArgument.getLastResult(), argumentStackSize, generalCount, true);
                if (generalCount >= maxGeneralCount) {
                    argumentStackSize += 8;
                }
                generalCount++;
                if (generalCount >= rcxLocation) {
                    generalQuads.addAll(currentArgument);
                } else {
                    currentArgument.addAll(generalQuads);
                    generalQuads = currentArgument;
                }
            }
        }

        argumentQuads.addAll(floatQuads);
        argumentQuads.addAll(generalQuads);

        return argumentStackSize;
    }
}
