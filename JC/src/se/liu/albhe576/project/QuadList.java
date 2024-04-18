package se.liu.albhe576.project;

import java.util.ArrayList;

public class QuadList extends ArrayList<Quad>{

    public QuadOp getLastOp(){return this.getLastQuad().op;}
    public Quad getLastQuad(){return this.get(this.size() - 1);}
    public void addQuad(QuadOp op, Symbol operand1, Symbol operand2, Symbol result){
        this.add(new Quad(op, operand1, operand2, result));
    }
    public void addQuad(Quad quad){
        this.add(quad);
    }

    public Symbol getLastResult(){
        return this.getLastQuad().result;
    }
    public void removeLastQuad(){this.remove(this.size() - 1);}
    public Symbol getLastOperand1(){return this.getLastQuad().operand1;}
    public Symbol getLastOperand2(){return this.get(this.size() - 1).operand2;}
    public void insertLabel(Symbol label){this.add(new Quad(QuadOp.LABEL, label, null, null));}

}
