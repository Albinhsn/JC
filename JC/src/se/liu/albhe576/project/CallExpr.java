package se.liu.albhe576.project;

import java.util.ArrayList;
import java.util.List;

public class CallExpr implements Expr{
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(name.literal + "(");
        for(int i = 0; i < args.size(); i++){
            s.append(args.get(i));
            if(i != args.size() - 1){
               s.append(", ");
            }
        }
        s.append(")");
        return s.toString();
    }

   private final Token name;
   private final List<Expr> args;
   public CallExpr(Token name, List<Expr> args){
       this.name = name;
       this.args = args;
   }

}
