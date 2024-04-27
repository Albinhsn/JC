package se.liu.albhe576.project;

import java.util.List;

public class ArrayStmt extends Stmt {
    private final ArrayDataType type;
    private final String name;
    private final List<Expr> items;
    private final int size;

    public ArrayStmt(ArrayDataType type, String name, List<Expr> items,int size, int line, String file){
        super(line, file);
        this.type = type;
        this.name = name;
        this.items = items;
        this.size = size;
    }

    @Override
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException {
        DataType itemType = type.itemType;

        int itemSize = SymbolTable.getStructSize(symbolTable.getStructs(), itemType);
        int offset = -(itemSize * this.size + symbolTable.getCurrentScopeSize());

        VariableSymbol arraySymbol = new VariableSymbol(name, DataType.getArray(itemType), offset, symbolTable.generateVariableId());
        symbolTable.addVariable(arraySymbol);

        for(int i = this.items.size() - 1; i >= 0; i--){
            Expr item = this.items.get(i);
            item.compile(symbolTable, quads);
            Symbol result = quads.getLastResult();

            if(itemType.canBeCastedTo(result.type)){
                result = AssignStmt.convertValue(result, Compiler.generateSymbol(itemType), quads);
            }

            if(!itemType.isSameType(result.type)){
                Compiler.error("Can't have different types in array declaration", line, file);
            }

            QuadList setupPointerQuads = new QuadList();
            Symbol addResult = setupPointerQuads.createSetupPointerOp(arraySymbol, itemSize * i);
            quads.createSetupBinary(setupPointerQuads, result, addResult);
            quads.createStore(result, addResult);
        }
    }
}
