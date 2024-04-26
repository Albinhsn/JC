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
            return String.format("lea rcx, [rsp + %d]\n", offset) +
                    this.moveStruct(argSymbol);
        }

        StringPair movePair = Quad.getMovOpAndRegisterFromType(argSymbol.type, 0);
        return String.format("%s [rsp + %d], %s",movePair.move(), offset, movePair.register());
    }

    int moveStructField(StringBuilder s, StructField field, int offset){
        if(field.type().isStruct()){
            Struct struct = this.structs.get(field.type().name);
            for(StructField f : struct.getFields()){
                offset = this.moveStructField(s, f, offset);
            }
            return offset;
        }

        String register = Quad.getRegisterFromType(field.type(), 2);
        s.append(String.format("mov %s, [rax + %d]\n", register, offset));
        s.append(String.format("mov [rcx + %d], %s\n", offset, register));
        return offset + SymbolTable.getStructSize(this.structs, field.type());
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
        int size = struct.getSize(this.structs);

        return String.format("sub rsp, %d\n", size) +
                "lea rcx, [rsp]" +
                this.moveStruct(structSymbol);
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
                StringPair movePair = Quad.getMovOpAndRegisterFromType(field.type(), 0);
                String move  = field.type().isStruct() ? "lea" : movePair.move();

                String out = String.format("%s %s, [rax + %d]",move, movePair.register(), this.getFieldOffset(struct, memberName));
                if(field.type().isByte()){
                    // ToDO hoist
                    out += String.format("\nmovzx %s, %s", Quad.getRegisterFromType(DataType.getInt(), 0), movePair.register());
                }
                return out;
            }
        }
        throw new CompileException(String.format("Couldn't find struct %s with member %s", type.name, memberName));
    }
    public String storeField(DataType type, Symbol memberSymbol) throws CompileException{
        StringPair movePair = Quad.getMovOpAndRegisterFromType(memberSymbol.type, 0);
        Struct struct       = this.structs.get(type.name);
        int offset          = this.getFieldOffset(struct, memberSymbol.name);

        if(memberSymbol.type.isStruct()){
            return String.format("lea rcx, [rcx + %d]\n", offset) + this.moveStruct(memberSymbol);
        }
        if(offset != 0){
            return String.format("%s [rcx + %d], %s", movePair.move(), offset, movePair.register());
        }
        return String.format("%s [rcx], %s", movePair.move(), movePair.register());
    }
    public String getVariableLocation(int offset){return offset >= 0 ? String.format("[rbp + %d]", offset) : String.format("[rbp %d]", offset);}
    public String loadVariable(int id) {
        VariableSymbol symbol = this.stackSymbols.get(id);
        StringPair movePair = Quad.getMovOpAndRegisterFromType(symbol.type, 0);
        return String.format("%s %s, %s", movePair.move(), movePair.register(), getVariableLocation(symbol.offset));
    }
    public String storeVariable(int id) {
        VariableSymbol symbol = this.stackSymbols.get(id);
        if(symbol.type.isStruct()){
            return this.moveStruct(symbol);
        }
        StringPair movePair = Quad.getMovOpAndRegisterFromType(symbol.type, 0);
        return String.format("%s %s, %s", movePair.move(), getVariableLocation(symbol.offset), movePair.register());
    }

    public Stack(Map<Integer, VariableSymbol> symbols, Map<String, Struct> structs) {
        this.stackSymbols = symbols;
        this.structs = structs;
    }
}
