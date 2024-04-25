package se.liu.albhe576.project;

import java.util.Comparator;

public class VariableSymbol extends Symbol implements Comparator<VariableSymbol> {
    public final int offset;
    public final int id;
    public VariableSymbol(String name, DataType type, int offset, int id){
       super(name, type);
       this.offset = offset;
       this.id = id;
    }

    @Override
    public int compare(VariableSymbol fst, VariableSymbol snd) {
        if(fst.offset > snd.offset){
           return 1;
        }else if(fst.offset < snd.offset){
            return -1;
        }
        return 0;
    }
}
