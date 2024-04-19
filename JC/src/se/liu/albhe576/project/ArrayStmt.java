package se.liu.albhe576.project;

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
    public void compile(SymbolTable symbolTable, QuadList quads) throws CompileException, UnknownSymbolException, UnexpectedTokenException, InvalidOperation {
        DataType itemType = type.getTypeFromPointer();

        int offset = symbolTable.getCurrentScopeSize();
        if(itemType.type == DataTypes.STRUCT){
            Struct struct = symbolTable.structs.get(itemType.name);
            offset -= struct.getSize(symbolTable.structs) * this.items.size();
        }else{
            offset -= 8 * this.items.size();
        }

        int depth = symbolTable.getDepth();
        VariableSymbol arraySymbol = new VariableSymbol(name, DataType.getArray(itemType), offset, depth);
        symbolTable.addSymbol(arraySymbol);

        for(int i = this.items.size() - 1; i >= 0; i--){
            Expr item = this.items.get(i);
            item.compile(symbolTable, quads);
            Symbol result = quads.getLastResult();
            quads.addQuad(QuadOp.PUSH, result, null, result);


            ImmediateSymbol immSymbol = Compiler.generateImmediateSymbol(DataType.getInt(), String.valueOf(8 * i));
            quads.addQuad(QuadOp.LOAD_IMM, immSymbol, null, Compiler.generateSymbol(DataType.getInt()));
            quads.addQuad(QuadOp.MOV_REG_CA, Compiler.generateSymbol(DataType.getInt()), null, Compiler.generateSymbol(DataType.getInt()));
            Symbol offsetSymbol = Compiler.generateSymbol(DataType.getInt());
            quads.addQuad(QuadOp.LOAD_POINTER, arraySymbol, null, Compiler.generateSymbol(arraySymbol.type));
            quads.addQuad(QuadOp.ADD, arraySymbol, offsetSymbol, arraySymbol);


            quads.addQuad(QuadOp.MOV_REG_CA, Compiler.generateSymbol(arraySymbol.type), null, Compiler.generateSymbol(arraySymbol.type));
            quads.addQuad(QuadOp.POP, null, null, Compiler.generateSymbol(itemType));
            quads.addQuad(QuadOp.STORE_INDEX, Compiler.generateSymbol(itemType), null, null);
            if(!itemType.isSameType(result.type)){
                throw new CompileException(String.format("Can't have different types in array declaration on line %d", this.line));
            }
        }
    }
}
