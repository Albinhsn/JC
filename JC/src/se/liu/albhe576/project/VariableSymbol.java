package se.liu.albhe576.project;

import java.util.Comparator;

public class VariableSymbol extends Symbol implements Comparator<VariableSymbol> {
    public int offset;
    public int depth;

    public VariableSymbol(String name, DataType type, int offset, int depth){
       super(name, type);
       this.offset = offset;
       this.depth =depth;
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
