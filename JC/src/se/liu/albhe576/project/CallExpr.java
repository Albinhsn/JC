package se.liu.albhe576.project;

import java.util.List;

public class CallExpr extends Expr{
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(name + "(");
        for(int i = 0; i < args.size(); i++){
            if(i != args.size() - 1){
               s.append(",");
            }
            s.append(args.get(i));
        }
        s.append(")");
        return s.toString();
    }

    Expr name;
   List<Expr> args;
   public CallExpr(Expr name, List<Expr> args){
       this.name = name;
       this.args = args;
   }
}
