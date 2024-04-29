package se.liu.albhe576.project;

import java.awt.*;
import java.util.Comparator;
import java.util.Map;

public class OptimizationResults implements Comparator<Map.Entry<String,Point>> {
    @Override
    public int compare(Map.Entry<String, Point> o1_, Map.Entry<String, Point> o2_) {
        Point o1 = o1_.getValue();
        Point o2 = o2_.getValue();
        return Integer.compare(o1.y, o2.y);
    }
}
