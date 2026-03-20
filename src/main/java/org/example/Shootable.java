package org.example;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;

import static org.example.SimpleMove.SCREEN_HEIGHT;

public abstract class Shootable extends Projectable{
    public boolean shot;
    public boolean flying;
    public float moveSpeed = 20f;

    @Override
    public Pair<Float> projectWithStrategy(Triple point, Triple rotatedPoint) {
        Pair<Float> projected;
       if(shot)
            projected = UtilProject.projectTo2D(rotatedPoint.x, rotatedPoint.y, rotatedPoint.z);
        else
            projected = UtilProject.projectTo2DWithoutRotatingAgainstCamera(point.x, point.y, point.z);
        return projected;
    }

    public abstract Triple[] getNodes();

    public abstract List<Pair<Integer>> getStaticEdges();

}
