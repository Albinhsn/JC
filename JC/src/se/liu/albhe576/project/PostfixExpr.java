package se.liu.albhe576.project;

public class PostfixExpr extends Expr{
    @Override
    public String toString() {
        return String.format("%s%s",target, op.literal());
    }

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
            this.error(String.format("Can't postfix type %s", target.type));
        }

        Symbol loadedSymbol = Compiler.generateSymbol(target.type);

        if(!loadedSymbol.type.isPointer()){
            if(op.type() == TokenType.TOKEN_INCREMENT){
                quads.createIncrement(loadedSymbol);
            }else{
                quads.createDecrement(loadedSymbol);
            }
        }else{
            // Add by the size of the underlying type rather than 1 to maintain correct alignment
            int structSize = SymbolTable.getStructSize(symbolTable.getStructs(), loadedSymbol.type);
            quads.createMovRegisterAToC(loadedSymbol);
            Symbol loadedImmediate = quads.createLoadImmediate(DataType.getInt(), String.valueOf(structSize));

            quads.createAdd(loadedSymbol, loadedImmediate);
        }

        // Remember to store the value after this in rax as well
        if(lastQuad.getOp() == QuadOp.DEREFERENCE){
            // This is the actual new value
            quads.createPush(target);

           // Recompile the target to resolve the store
            this.target.compile(symbolTable, quads);
            Quad lastTargetQuad = quads.pop();
            Symbol dereferenced = lastTargetQuad.getOperand1();

            quads.createMovRegisterAToC(dereferenced);
            quads.createPop(target);
            quads.createStoreIndex(target, dereferenced);
        }else if(lastQuad.getOp() == QuadOp.GET_FIELD){
            quads.createPush(target);
            this.target.compile(symbolTable, quads);
            Quad lastTargetQuad = quads.pop();
            Symbol dereferenced = lastTargetQuad.getOperand1();
            Symbol member = lastTargetQuad.getOperand2();

            quads.createMovRegisterAToC(dereferenced);
            quads.createPop(target);
            quads.createSetField(member, dereferenced);

        }else if(lastQuad.getOp() == QuadOp.INDEX){
            // This is the actual new value
            quads.createPush(target);

            // Recompile the target to resolve the store
            this.target.compile(symbolTable, quads);
            Quad lastTargetQuad = quads.pop();
            Symbol dereferenced = lastTargetQuad.getOperand2();
            Symbol index = lastTargetQuad.getOperand1();

            quads.createMovRegisterAToC(dereferenced);
            quads.createPop(target);
            quads.createStoreIndex(index, dereferenced);
        }else{
            quads.createStore(lastQuad.getOperand1());
        }
    }
}
