package se.liu.albhe576.project;

import java.util.Map;

public class Stack {
    private final Map<Integer, VariableSymbol> stackSymbols;
    private final Map<String, Struct> structs;

    public Map<String, Struct> getStructs(){return this.structs;}

    public String loadFieldPointer(int variableId, String field) throws CompileException {
        VariableSymbol variable = this.stackSymbols.get(variableId);
        Struct struct = this.getStructs().get(variable.type.name);
        int offset = this.getFieldOffset(struct, field);
        if(offset == 0){
            return "lea rax, [rax]";
        }else{
            return String.format("lea rax, [rax + %d]", offset);
        }
    }
    public String loadVariablePointer(int id) {
        VariableSymbol symbol = this.stackSymbols.get(id);
        if(symbol.offset < 0){
            return String.format("lea rax, [rbp %d]", symbol.offset);
        }else{
            return String.format("lea rax, [rbp + %d]", symbol.offset);
        }
    }

    public String moveArg(Symbol argSymbol, int offset){
        if(argSymbol.type.isStruct()){
            StringBuilder s = new StringBuilder();
            s.append(String.format("lea rcx, [rsp + %d]\n", offset));
            s.append(this.moveStruct(argSymbol));
            return s.toString();
        }

        String move = Quad.getMovOpFromType(argSymbol.type);
        String register = Quad.getRegisterFromType(argSymbol.type, 0);

        return String.format("%s [rsp + %d], %s",move, offset, register);
    }

    int moveStructField(StringBuilder s, StructField field, int offset){

        if(field.type().isStruct()){
            Struct struct = this.structs.get(field.type().name);
            for(StructField f : struct.getFields()){
                offset = this.moveStructField(s, f, offset);
            }
        }else if(field.type().isByte()){
            s.append(String.format("mov bl, [rax + %d]\n", offset));
            s.append(String.format("mov [rcx + %d], bl\n", offset));
            offset += 1;
        }else{
            s.append(String.format("mov rbx, [rax + %d]\n", offset));
            s.append(String.format("mov [rcx + %d], rbx\n", offset));
            offset += 8;
        }
        return offset;
    }

    public String moveStruct(Symbol value){
        Struct valueStruct  = this.structs.get(value.type.name);

        StringBuilder s = new StringBuilder();

        int offset = 0;
        for(StructField field : valueStruct.getFields()){
            offset = this.moveStructField(s, field, offset);
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
        for(StructField field : struct.getFields()){
            if(field.name().equals(memberName)){
                return size;
            }
            size += SymbolTable.getStructSize(this.structs, field.type());
        }
        throw new CompileException(String.format("Couldn't find member %s?\n", memberName));
    }
    public String loadField(DataType type, String memberName) throws CompileException {
        Struct struct = this.structs.get(type.name);
        for(StructField field : struct.getFields()){
            if(field.name().equals(memberName)){
                String move = Quad.getMovOpFromType(field.type());
                String register = Quad.getRegisterFromType(field.type(), 0);
                move = field.type().isStruct() ? "lea" : move;
                int offset = this.getFieldOffset(struct, memberName);
                String out = String.format("%s %s, [rax + %d]",move, register, offset);
                if(field.type().isByte()){
                    out += String.format("\nmovzx %s, %s", Quad.getRegisterFromType(DataType.getInt(), 0), register);
                }
                return out;
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
    public String loadVariable(int id) {
        VariableSymbol stackSymbol = this.stackSymbols.get(id);
        if(stackSymbol.offset < 0){
            return this.loadLocal(stackSymbol);
        }
        return this.loadArgument(stackSymbol);
    }
    public String storeVariable(int id) {
        VariableSymbol symbol = this.stackSymbols.get(id);
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

        if(symbol.type.isStruct()){
            return this.moveStruct(symbol);
        }

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

    public Stack(Map<Integer, VariableSymbol> symbols, Map<String, Struct> structs) {
        this.stackSymbols = symbols;
        this.structs = structs;
    }
}
