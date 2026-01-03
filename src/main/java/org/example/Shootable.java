package org.example;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;

import static org.example.SimpleMove.SCREEN_HEIGHT;

public abstract class Shootable extends Projectable{
    public boolean shot;
    public boolean flying;
    public Triple direction;
    public float r = 0.3f;

    @Override
    public Pair<Float> projectWithStrategy(SimpleMove simpleMove, Triple point, Triple rotatedPoint) {
        Pair<Float> projected;
       if(shot)
            projected = simpleMove.projectTo2D(rotatedPoint.x, rotatedPoint.y, rotatedPoint.z);
        else
            projected = simpleMove.projectTo2DWithoutRotatingAgainstCamera(point.x, point.y, point.z);
        return projected;
    }

    public void drawEdges(Graphics2D g, SimpleMove simpleMove) {
        Pair<Float>[] projectedDotsForGun = getProjectedDots(simpleMove);

        for(Pair<Integer> pair : getStaticEdges()){
            if(projectedDotsForGun[pair.x] == null || projectedDotsForGun[pair.y] == null) continue;
            g.draw(new Line2D.Float(
                    projectedDotsForGun[pair.x].x,
                    SCREEN_HEIGHT - projectedDotsForGun[pair.x].y,// - because panel y starts from top
                    projectedDotsForGun[pair.y].x,
                    SCREEN_HEIGHT - projectedDotsForGun[pair.y].y));// - because panel y starts from top
        }

    }

    public abstract Triple[] getNodes();

    public abstract List<Pair<Integer>> getStaticEdges();
}
