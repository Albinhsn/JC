package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class ArrayStmt extends Stmt {

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[");
        for(int i = 0; i < items.size(); i++){
            s.append(items.get(i));
            if(i < items.size() - 1){
                s.append(", ");
            }
        }
        s.append("]");
        return s.toString();
    }

    public final DataType type;
    public final String name;
    private final List<Expr> items;

    public ArrayStmt(DataType type, String name, List<Expr> items, int line){
        super(line);
        this.type = type;
        this.name = name;
        this.items = items;
    }

    @Override
    public QuadList compile(SymbolTable symbolTable) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {
        QuadList quads = new QuadList();
        DataType itemType = type.getTypeFromPointer();

        int offset = symbolTable.getCurrentScopeSize();
        if(itemType.type == DataTypes.STRUCT){
            Struct struct = symbolTable.structs.get(itemType.name);
            offset -= struct.getSize(symbolTable.structs) * this.items.size();
        }else{
            offset -= 8 * this.items.size();
        }
        int depth = symbolTable.getDepth();
        VariableSymbol arraySymbol = new VariableSymbol(name, type, offset, depth);
        symbolTable.addSymbol(arraySymbol);

        for(Expr item : this.items){
            quads.concat(item.compile(symbolTable));
            quads.addQuad(QuadOp.PUSH, null, null, quads.getLastResult());
            if(!itemType.isSameType(quads.getLastResult().type)){
                throw new CompileException(String.format("Can't have different types in array declaration on line %d", this.line));
            }
        }
        return quads;
    }
}
