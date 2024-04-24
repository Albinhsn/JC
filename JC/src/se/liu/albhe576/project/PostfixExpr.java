package se.liu.albhe576.project;

public class PostfixExpr extends Expr{
    private final Expr target;
    private final Token op;

    public PostfixExpr(Expr target, Token op, int line, String file){
        super(line, file);
        this.target = target;
        this.op   = op;
    }

    private static boolean isInvalidPostfixTarget(DataType type){
        return type.isArray() || type.isString() || type.isStruct();
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        this.target.compile(symbolTable, quads);
        Quad lastQuad = quads.getLastQuad();
        Symbol target = lastQuad.getResult();

        if(isInvalidPostfixTarget(target.type)){
            Compiler.error(String.format("Can't postfix type %s", target.type), line, file);
        }

        Symbol loadedSymbol = Compiler.generateSymbol(target.type);

        if(loadedSymbol.type.isPointer()){
            // Add by the size of the underlying type rather than 1 to maintain correct alignment
            int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), loadedSymbol.type);
            quads.createMovRegisterAToC(loadedSymbol);
            Symbol loadedImmediate = quads.createLoadImmediate(DataType.getInt(), String.valueOf(structSize));

            quads.createAdd(loadedSymbol, loadedImmediate);

        }else if(op.type() == TokenType.TOKEN_INCREMENT){
                quads.createIncrement(loadedSymbol);
        }else{
                quads.createDecrement(loadedSymbol);
        }

        QuadList varQuads = new QuadList();
        this.target.compile(symbolTable, varQuads);

        switch(lastQuad.getOp()){
            case DEREFERENCE -> {AssignStmt.compileStoreDereferenced(quads, varQuads);}
            case GET_FIELD-> {AssignStmt.compileStoreField(quads, varQuads);}
            case INDEX -> {AssignStmt.compileStoreIndex(quads, varQuads);}
            default -> {quads.createStore(lastQuad.getOperand1());}
        }
    }
}
