package se.liu.albhe576.project;

import java.util.List;
import java.util.Map;

public class Function {

    private final int line;
    private final String file;
    private final List<StructField> arguments;
    private final DataType returnType;
    private final List<Stmt> body;
    public List<StructField> getArguments(){return this.arguments;}
    public int getLine(){return this.line;}
    public String getFile(){return this.file;}
    public boolean isExternal(){return this.body == null;}
    public List<Stmt> getBody(){return this.body;}
    public DataType getReturnType(){return this.returnType;}
    public Symbol getFunctionSymbol(String name){return new Symbol(name, returnType);}
    public Function(List<StructField> arguments, DataType returnType, List<Stmt> stmts, String file, int line){
        this.arguments = arguments;
        this.returnType = returnType;
        this.body = stmts;
        this.file = file;
        this.line = line;
    }

    public int call(SymbolTable symbolTable, QuadList argumentQuads, List<Expr> args) throws CompileException {
        int argumentStackSize = 0;
        for(int i = 0 ; i < args.size(); i++){
            Expr argument = args.get(i);
            argument.compile(symbolTable, argumentQuads);
            DataType paramType = this.arguments.get(i).type();
            int argumentSize = symbolTable.getStructSize(paramType);

            Symbol result = argumentQuads.getLastResult();

            if(!paramType.isSameType(result.type) && !paramType.canBeConvertedTo(result.type)){
                Compiler.error(String.format("Parameter mismatch expected %s got %s", paramType, result.type), line, file);
            }else if(!paramType.isSameType(result.type)){
                result = argumentQuads.createConvert(symbolTable, result, paramType);
            }

            argumentQuads.createParam(result, argumentStackSize, 0, false);
            argumentStackSize += argumentSize;
        }
        return argumentStackSize;
    }
}
