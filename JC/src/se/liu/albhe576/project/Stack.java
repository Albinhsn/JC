package se.liu.albhe576.project;

import java.util.Map;

public class Stack {
    private final Map<String, VariableSymbol> stackSymbols;
    public final Map<String, Struct> structs;

    public Struct getStruct(String name){
        return this.structs.get(name);
    }

    public String loadVariablePointer(String name) {
        VariableSymbol symbol = this.stackSymbols.get(name);
        if(symbol.offset < 0){
            return String.format("lea rax, [rbp %d]", symbol.offset);
        }else{
            return String.format("lea rax, [rbp + %d]", symbol.offset);
        }
    }

    public String moveStruct(Symbol value){
        Struct valueStruct  = this.structs.get(value.type.name);

        StringBuilder s = new StringBuilder();
        int size = valueStruct.getSize(this.structs);

        s.append("mov rbx, [rax]\n");
        s.append("mov [rcx], rbx\n");

        int sizeInBytes = size / 8;

        for(int i = 1; i < sizeInBytes; i++){
            int offset = i * 8;
            s.append(String.format("mov rbx, [rax + %d]\n", offset));
            s.append(String.format("mov [rcx + %d], rbx", offset));

            if(i != sizeInBytes - 1){
                s.append("\n");
            }
        }

        return s.toString();

    }

    public String pushStruct(Symbol structSymbol){
        Struct struct = this.structs.get(structSymbol.type.name);
        StringBuilder s = new StringBuilder();
        int size = struct.getSize(this.structs);
        s.append(String.format("sub rsp, %d\n", size));
        int sizeInBytes = size / 8;


        s.append("mov rcx, [rax]\n");
        s.append("mov [rsp], rcx\n");

        for(int i = 1; i < sizeInBytes; i++){
            int offset = i * 8;
            s.append(String.format("mov rcx, [rax + %d]\n", offset));
            s.append(String.format("mov [rsp + %d], rcx", offset));

            if(i != sizeInBytes - 1){
                s.append("\n");
            }
        }

        return s.toString();
    }

    private int getFieldOffset(Struct struct, String memberName)throws CompileException  {
        int size = 0;
        for(StructField field : struct.fields){
            if(field.name.equals(memberName)){
                return size;
            }
            size += 8;
        }
        throw new CompileException(String.format("Couldn't find member %s?\n", memberName));
    }
    public String loadField(DataType type, String memberName) throws CompileException {
        Struct struct = this.structs.get(type.name);
        for(StructField field : struct.fields){
            if(field.name.equals(memberName)){
                String op = field.type.isStruct() ? "lea" : "mov";
                int offset = this.getFieldOffset(struct, memberName);
                if(offset == 0){
                    return String.format("%s rax, [rax]", op);
                }
                return String.format("%s rax, [rax + %d]",op, offset);
            }
        }
        throw new CompileException(String.format("Couldn't find struct %s with member %s", type.name, memberName));
    }
    public String storeField(DataType type, Symbol memberSymbol) throws CompileException{
        String move = Quad.getMovOpFromType(memberSymbol.type);
        String register = Quad.getRegisterFromType(memberSymbol.type, 0);

        Struct struct  = this.structs.get(type.name);
        int offset = this.getFieldOffset(struct, memberSymbol.name);

        if(memberSymbol.type.isStruct()){
            Struct memberStruct = this.structs.get(memberSymbol.type.name);
            StringBuilder s = new StringBuilder();
            int size = memberStruct.getSize(this.structs);

            s.append("mov rbx, [rax]\n");
            s.append(String.format("mov [rcx + %d], rbx\n", offset));

            int sizeInBytes = size / 8;

            for(int i = 1; i < sizeInBytes; i++){
                int memberOffset = i * 8;
                s.append(String.format("mov rbx, [rax + %d]\n", memberOffset));
                s.append(String.format("mov [rcx + %d], rbx", memberOffset + offset));

                if(i != sizeInBytes - 1){
                    s.append("\n");
                }
            }

            return s.toString();
        }else{
            if(offset != 0){
                return String.format("%s [rcx + %d], %s", move, offset, register);
            }
            return String.format("%s [rcx], %s", move, register);
        }
    }
    public String loadVariable(String name) {
        VariableSymbol stackSymbol = this.stackSymbols.get(name);
        if(stackSymbol.offset < 0){
            return this.loadLocal(stackSymbol);
        }
        return this.loadArgument(stackSymbol);
    }
    public String storeVariable(String name) {
        VariableSymbol symbol = this.stackSymbols.get(name);
        if(symbol.offset < 0){
            return this.storeLocal(symbol);
        }
        return this.storeArgument(symbol);
    }

    private String loadArgument(VariableSymbol symbol){
        int offset = this.getStackPointerOffset(symbol);
        String move = Quad.getMovOpFromType(symbol.type);
        String register = Quad.getRegisterFromType(symbol.type, 0);

        return String.format("%s %s, [rbp + %d]", move, register, offset);
    }
    private String storeArgument(VariableSymbol symbol){
        int offset = this.getStackPointerOffset(symbol);
        String move = Quad.getMovOpFromType(symbol.type);
        String register = Quad.getRegisterFromType(symbol.type, 0);
        return String.format("%s [rbp + %d], %s", move, offset, register);
    }

    private String loadLocal(VariableSymbol symbol){
        String move = Quad.getMovOpFromType(symbol.type);
        String register = Quad.getRegisterFromType(symbol.type, 0);
        if(symbol.offset == 0){
            return String.format("%s %s, [rbp]", move, register);
        }
        return String.format("%s %s, [rbp %d]", move, register, symbol.offset);
    }

    private String storeLocal(VariableSymbol symbol){
        int offset = this.getStackPointerOffset(symbol);
        String move = Quad.getMovOpFromType(symbol.type);
        String register = Quad.getRegisterFromType(symbol.type, 0);
        if(offset == 0){
            return String.format("%s [rsp], %s", move, register);
        }
        return String.format("%s [rbp %d], %s", move, offset, register);
    }

    private int getStackPointerOffset(VariableSymbol symbol){
        return symbol.offset;
    }

    public Stack(Map<String, VariableSymbol> symbols, Map<String, Struct> structs) throws CompileException {
        this.stackSymbols = symbols;
        this.structs = structs;
    }
}
