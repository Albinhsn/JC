package se.liu.albhe576.project;

public class Value {
    @Override
    public String toString() {
        return Integer.toString(location);
    }

    public int location;

    public Value(int location){
        this.location = location;
    }
}
