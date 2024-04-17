package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class Stack {

    static class StackSymbol{
        private final int offset;
        private final DataType type;
        private final String name;
        private int getFieldOffset(List<Struct> structs, String name) throws UnknownSymbolException {
            // ToDo wtf are you doing
            if(type.type != DataTypes.STRUCT){
                return 0;
            }
            Struct struct = SymbolTable.lookupStruct(structs, this.type.name);
            int size = 0;
            for(StructField field : struct.fields){
                if(field.name.equals(name)){
                    return size;
                }
                size += 8;
            }
            return 0;
        }
        StackSymbol(int offset, DataType type, String name){
            this.type   = type;
            this.name   = name;
            this.offset = offset;
        }
    };

    private final List<StackSymbol> stackSymbols;
    private final List<Struct> structs;

    public String loadStructPointer(String name) throws UnknownSymbolException {
        StackSymbol symbol = this.findSymbol(name);
        if(symbol.offset < 0){
            return String.format("lea rax, [rbp %d]", symbol.offset);
        }else{
            return String.format("lea rax, [rbp + %d]", symbol.offset);
        }
    }
    public String loadField(String variableName, String memberName) throws UnknownSymbolException {
        StackSymbol variable = this.findSymbol(variableName);
        int offset = variable.getFieldOffset(structs, memberName);

        if(offset == 0){
            return "mov rax, [rax]";
        }
        return String.format("mov rax, [rax + %d]", offset);
    }
    public String storeField(Symbol variableSymbol, Symbol memberSymbol) throws UnknownSymbolException{
        StackSymbol variable = this.findSymbol(variableSymbol.name);
        int offset = variable.getFieldOffset(structs, memberSymbol.name);
        String move = memberSymbol.type.type == DataTypes.FLOAT ? "movss" : "mov";
        String register = this.getRegisterFromType(memberSymbol.type.type, 0);
        if(offset != 0){
            return String.format("%s [rax + %d], %s", move, offset, register);
        }
        return String.format("%s [rax], %s", move, register);
    }
    private boolean symbolExists(String name){
        for(StackSymbol stackSymbol : this.stackSymbols){
            if(stackSymbol.name.equals(name)){
                return true;
            }
        }
        return false;
    }

    private StackSymbol findSymbol(String name) throws UnknownSymbolException{
        for(StackSymbol stackSymbol : this.stackSymbols){
            if(stackSymbol.name.equals(name)){
                return stackSymbol;
            }
        }
        throw new UnknownSymbolException(String.format("Couldn't find symbol '%s' on the stack", name));
    }


    public int getLocalSize() throws UnknownSymbolException {
        int size = 0;
        for(StackSymbol stackSymbol : this.stackSymbols){
            if(stackSymbol.type.type != DataTypes.STRUCT){
                size -= 8;
            }else{
                Struct struct = SymbolTable.lookupStruct(structs, stackSymbol.type.name);
                size -= struct.getSize();
            }
        }
        return size;
    }
    public String loadVariable(String name, QuadOp prevOp) throws UnknownSymbolException {
        StackSymbol stackSymbol = this.findSymbol(name);
        if(stackSymbol.offset < 0){
            return this.loadLocal(stackSymbol, prevOp);
        }
        return this.loadArgument(stackSymbol, prevOp);

    }

    private int getStructSize(DataType type) throws UnknownSymbolException {
        if(type.type == DataTypes.STRUCT){
            Struct structType = SymbolTable.lookupStruct(this.structs, type.name);
            return structType.getSize();
        }
        return  8;
    }
    public String storeVariable(DataType type, String name) throws UnknownSymbolException {
        if(this.symbolExists(name)) {
            StackSymbol symbol = this.findSymbol(name);
            if(symbol.offset < 0){
                return this.storeLocal(symbol);
            }
            return this.storeArgument(symbol);
        }

        int structSize = this.getStructSize(type);
        int offset = this.getLocalSize() - structSize;
        this.stackSymbols.add(new StackSymbol(offset, type, name));

        if(structSize > 8){
            return String.format("sub rsp, %d", structSize);
        }
        if(type.type == DataTypes.FLOAT){
            return "sub rsp, 8\nmovss [rsp], xmm0";
        }
        return "push rax";
    }

    private String loadArgument(StackSymbol symbol, QuadOp prevOp){
        int offset = this.getStackPointerOffset(symbol);
        int registerIndex = (prevOp == QuadOp.LOAD_IMM || prevOp == QuadOp.LOAD) ? 1 : 0;
        String move = symbol.type.type == DataTypes.FLOAT ? "movss" : "mov";
        String register = this.getRegisterFromType(symbol.type.type, registerIndex);

        return String.format("%s %s, [rbp + %d]", move, register, offset);

    }
    private String storeArgument(StackSymbol symbol){
        int offset = this.getStackPointerOffset(symbol);
        String move = symbol.type.type == DataTypes.FLOAT ? "movss" : "mov";
        String register = this.getRegisterFromType(symbol.type.type, 0);
        return String.format("%s [rbp + %d], %s", move, offset, register);
    }
    private String getRegisterFromType(DataTypes type, int registerIndex){
        final String[] floatRegisters = new String[]{"xmm0", "xmm1"};
        final String[] generalRegisters = new String[]{"rax", "rcx"};
        if(type == DataTypes.FLOAT){
            return floatRegisters[registerIndex];
        }
        return generalRegisters[registerIndex];
    }

    private String loadLocal(StackSymbol symbol, QuadOp prevOp){
        int offset = this.getStackPointerOffset(symbol);
        int registerIndex = (prevOp == QuadOp.LOAD_IMM || prevOp == QuadOp.LOAD) ? 1 : 0;
        String move = symbol.type.type == DataTypes.FLOAT ? "movss" : "mov";
        String register = this.getRegisterFromType(symbol.type.type, registerIndex);
        if(offset == 0){
            return String.format("%s %s, [rbp]", move, register);
        }
        return String.format("%s %s, [rbp %d]", move, register, offset);
    }

    private String storeLocal(StackSymbol symbol){
        int offset = this.getStackPointerOffset(symbol);
        String move = symbol.type.type == DataTypes.FLOAT ? "movss" : "mov";
        String register = this.getRegisterFromType(symbol.type.type, 0);
        if(offset == 0){
            return String.format("%s [rsp], %s", move, register);
        }
        return String.format("%s [rbp %d], %s", move, offset, register);
    }

    private int getStackPointerOffset(StackSymbol symbol){
        return symbol.offset;
    }

    public Stack(List<StructField> arguments, List<Struct> structs) throws UnknownSymbolException {
        this.stackSymbols = new ArrayList<>();
        this.structs = structs;

        for(int i = 0, offset = 16; i < arguments.size(); i++){
            StructField argField = arguments.get(i);
            String name = argField.name;
            this.stackSymbols.add(new StackSymbol(offset, argField.type, name));

            offset += this.getStructSize(argField.type);
        }

    }
}
