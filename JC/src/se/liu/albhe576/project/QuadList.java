package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class QuadList {
    private List<Quad> quads;

    public int size(){
        return quads.size();
    }
    public Quad get(int index){
        return quads.get(index);
    }
    public List<Quad> getQuads(){
        return quads;
    }

    public QuadOp getLastOp(){
        return this.getLastQuad().op;
    }
    public Quad getLastQuad(){
        return this.quads.get(this.quads.size() - 1);
    }

    public void concat(QuadList other){
        this.quads.addAll(other.quads);
    }
    public void addQuad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){
        this.quads.add(new Quad(op, operand1, operand2, result));
    }
    public void addQuad(Quad quad){
        this.quads.add(quad);
    }

    public Symbol getLastResult(){
        return this.quads.get(this.quads.size() - 1).result;
    }
    public void removeLastQuad(){
        this.quads.remove(this.quads.size() - 1);
    }
    public Symbol getLastOperand1(){
        return this.quads.get(this.quads.size() - 1).operand1;
    }
    public void insertLabel(Symbol label){
        this.quads.add(new Quad(QuadOp.LABEL, label, null, null));
    }

    public QuadList(){
        this.quads = new ArrayList<>();
    }
}
