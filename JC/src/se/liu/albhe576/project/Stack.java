package se.liu.albhe576.project;

import java.util.List;
import java.util.Map;

public class Stack {
    private final Map<String, VariableSymbol> stackSymbols;
    public final Map<String, Struct> structs;

    public Struct getStruct(String name){
        return this.structs.get(name);
    }

    public String loadStructPointer(String name) {
        VariableSymbol symbol = this.stackSymbols.get(name);
        if(symbol.offset < 0){
            return String.format("lea rax, [rbp %d]", symbol.offset);
        }else{
            return String.format("lea rax, [rbp + %d]", symbol.offset);
        }
    }

    public void debug(){
        if(stackSymbols.isEmpty()){
            System.out.println("Empty stack");
            return;
        }
        List<VariableSymbol> locals = new java.util.ArrayList<>(stackSymbols.values().stream().toList());
        locals.sort(locals.get(0));
        int prev = -1;
        for(VariableSymbol local : locals){
            if(prev != -1){
                while(prev != local.offset){
                    prev += 8;
                    if(prev != local.offset){
                        System.out.printf("%d XXX\n", prev);
                    }
                }
            }
            System.out.println(local.offset + " " + local.name);
            prev = local.offset;

        }
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

    private int getFieldOffset(Struct struct, String memberName)throws UnknownSymbolException  {
        int size = 0;
        for(StructField field : struct.fields){
            if(field.name.equals(memberName)){
                return size;
            }
            size += 8;
        }
        throw new UnknownSymbolException(String.format("Couldn't find member %s?\n", memberName));
    }
    public String loadField(DataType type, String memberName) throws UnknownSymbolException {
        Struct struct = this.structs.get(type.name);
        for(StructField field : struct.fields){
            if(field.name.equals(memberName)){
                String op = field.type.type == DataTypes.STRUCT ? "lea" : "mov";
                int offset = this.getFieldOffset(struct, memberName);
                if(offset == 0){
                    return String.format("%s rax, [rax]", op);
                }
                return String.format("%s rax, [rax + %d]",op, offset);
            }
        }
        throw new UnknownSymbolException(String.format("Couldn't find struct %s of type %s", type.name, memberName));
    }
    public String storeField(DataType type, Symbol memberSymbol) throws UnknownSymbolException{
        String move = memberSymbol.type.type == DataTypes.FLOAT ? "movsd" : "mov";
        String register = this.getRegisterFromType(memberSymbol.type.type, 1);
        Struct struct  = this.structs.get(type.name);
        int offset = this.getFieldOffset(struct, memberSymbol.name);
        if(offset != 0){
            return String.format("%s [rax + %d], %s", move, offset, register);
        }
        return String.format("%s [rax], %s", move, register);
    }
    public int getLocalSize() {
        List<VariableSymbol> symbols = this.stackSymbols.values().stream().toList();
        if(symbols.isEmpty()){
            return 0;
        }
        return symbols.get(symbols.size() - 1).offset;
    }

    public String loadVariable(String name, QuadOp prevOp) {
        VariableSymbol stackSymbol = this.stackSymbols.get(name);
        if(stackSymbol.offset < 0){
            return this.loadLocal(stackSymbol, prevOp);
        }
        return this.loadArgument(stackSymbol, prevOp);
    }
    public String storeVariable(String name) {
        VariableSymbol symbol = this.stackSymbols.get(name);
        if(symbol.offset < 0){
            return this.storeLocal(symbol);
        }
        return this.storeArgument(symbol);
    }

    private String loadArgument(VariableSymbol symbol, QuadOp prevOp){
        int offset = this.getStackPointerOffset(symbol);
        int registerIndex = prevOp != null && prevOp.isLoad() ? 1 : 0;
        String move = symbol.type.type == DataTypes.FLOAT ? "movsd" : "mov";
        String register = this.getRegisterFromType(symbol.type.type, registerIndex);

        return String.format("%s %s, [rbp + %d]", move, register, offset);
    }
    private String storeArgument(VariableSymbol symbol){
        int offset = this.getStackPointerOffset(symbol);
        String move = symbol.type.type == DataTypes.FLOAT ? "movsd" : "mov";
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

    private String loadLocal(VariableSymbol symbol, QuadOp prevOp){
        int registerIndex = prevOp.isLoad() ? 1 : 0;
        String move = symbol.type.type == DataTypes.FLOAT ? "movsd" : "mov";
        String register = this.getRegisterFromType(symbol.type.type, registerIndex);
        if(symbol.offset == 0){
            return String.format("%s %s, [rbp]", move, register);
        }
        return String.format("%s %s, [rbp %d]", move, register, symbol.offset);
    }

    private String storeLocal(VariableSymbol symbol){
        int offset = this.getStackPointerOffset(symbol);
        String move = symbol.type.type == DataTypes.FLOAT ? "movsd" : "mov";
        String register = this.getRegisterFromType(symbol.type.type, 0);
        if(offset == 0){
            return String.format("%s [rsp], %s", move, register);
        }
        return String.format("%s [rbp %d], %s", move, offset, register);
    }

    private int getStackPointerOffset(VariableSymbol symbol){
        return symbol.offset;
    }

    public Stack(Map<String, VariableSymbol> symbols, Map<String, Struct> structs) throws UnknownSymbolException {
        this.stackSymbols = symbols;
        this.structs = structs;
    }
}
