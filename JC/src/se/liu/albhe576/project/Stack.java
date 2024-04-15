package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class Stack {

    static class StackSymbol{
        private final int offset;
        private final StructSymbol type;
        private final String name;
        private int getFieldOffset(String name){
            int size = 0;
            for(StructField field : type.fields){
                if(field.name.equals(name)){
                    System.out.printf("FOUND FIELD %s\n", name);
                    return size;
                }
                size += 8;
            }
            System.out.printf("DIDN*T FIND FIELD %s\n", name);
            return 0;
        }
        StackSymbol(int offset, StructSymbol type, String name){
            this.type   = type;
            this.name   = name;
            this.offset = offset + type.getSize();
        }
    };

    private final List<StackSymbol> stackSymbols;

    public String loadStructPointer(String name) throws UnknownSymbolException {

        StackSymbol symbol = this.findSymbol(name);
        return String.format("lea rax, [rsp + %d]", symbol.offset);
    }
    public String loadField(String variableName, String memberName) throws UnknownSymbolException {
        StackSymbol variable = this.findSymbol(variableName);
        int offset = this.getStackPointerOffset(variable) + variable.getFieldOffset(memberName);

        if(offset != 0){
            return String.format("mov rax, [rax]");
        }
        return String.format("mov rax, [rax + %d]", offset);
    }
    public String storeField(String variableName, String memberName) throws UnknownSymbolException{

        StackSymbol variable = this.findSymbol(variableName);
        int offset = variable.getFieldOffset(memberName);

        if(offset != 0){
            return String.format("mov [rax + %d], rcx", offset);
        }
        return "mov [rax], rcx";
    }

    private StackSymbol findSymbol(String name) throws UnknownSymbolException{
        for(StackSymbol stackSymbol : this.stackSymbols){
            if(stackSymbol.name.equals(name)){
                return stackSymbol;
            }
        }
        throw new UnknownSymbolException(String.format("Couldn't find symbol '%s' on the stack", name));
    }


    public int getLocalSize(){
        int size = 0;
        System.out.printf("Got %d symbols on the stack\n", this.stackSymbols.size());
        for(StackSymbol stackSymbol : this.stackSymbols){
                size += stackSymbol.type.getSize();
                System.out.printf("Added %d\n", size);
        }
        System.out.printf("And ended up with %d in size\n", size);
        return size;
    }
    public String loadVariable(String name, QuadOp prevOp) throws UnknownSymbolException {
        StackSymbol stackSymbol = this.findSymbol(name);
        if(stackSymbol.offset > 0){
            return this.loadLocal(stackSymbol, prevOp);
        }
        return this.loadArgument(stackSymbol, prevOp);

    }
    public String storeVariable(StructSymbol type, String name) {
        try{
            StackSymbol symbol = this.findSymbol(name);
            if(symbol.offset < 0){
                return this.storeLocal(symbol);
            }
            return this.storeArgument(symbol);
        }catch (UnknownSymbolException e){
            int size = this.getLocalSize();
            this.stackSymbols.add(new StackSymbol(size, type, name));
            return "push rax";
        }

    }

    private String loadArgument(StackSymbol symbol, QuadOp prevOp){
        int offset = this.getStackPointerOffset(symbol);
        if(prevOp == QuadOp.LOAD_IMM || prevOp == QuadOp.LOAD){
            return String.format("mov rcx, [rsp + %d]", offset);
        }
        return String.format("mov rax, [rsp + %d]", offset);

    }
    private String storeArgument(StackSymbol symbol){
        int offset = this.getStackPointerOffset(symbol);
        return String.format("mov [rsp + %d], rax", offset);
    }

    private String loadLocal(StackSymbol symbol, QuadOp prevOp){
        int offset = this.getStackPointerOffset(symbol);
        if(prevOp == QuadOp.LOAD_IMM || prevOp == QuadOp.LOAD){
            if(offset == 0){
                return "mov rcx, [rsp]";
            }
            return String.format("mov rcx, [rsp + %d]", offset);
        } else if(offset == 0){
            return "mov rax, [rsp]";
        }
        return String.format("mov rax, [rsp + %d]", offset);

    }
    private String storeLocal(StackSymbol symbol){
        int offset = this.getStackPointerOffset(symbol);
        if(offset == 0){
            return "mov [rsp], rax";
        }
        return String.format("mov [rsp - %d], rax", offset);
    }

    private int getStackPointerOffset(StackSymbol symbol){
        return symbol.offset;
    }

    public Stack(List<StructSymbol> arguments, List<String> names){
        this.stackSymbols = new ArrayList<>();

        for(int i = 0, offset = 8; i < arguments.size(); i++){
            StructSymbol arg = arguments.get(i);
            String name = names.get(i);
            this.stackSymbols.add(new StackSymbol(offset, arg, name));
            offset += arg.getSize();
        }

    }
}
