package org.example;

import java.awt.*;
import java.awt.geom.Line2D;

import static org.example.SimpleMove.SCREEN_HEIGHT;

public abstract class Projectable {
    float x;
    float y;
    float z;

    Triple[] nodes;

    Pair<Float> rotation = new Pair<>(0f,0f);

    public void draw(Graphics2D g, SimpleMove simpleMove) {
        Pair<Float>[] projectedDots = getProjectedDots(simpleMove);

        for(Pair<Integer> pair : Gun.edges){
            if(projectedDots[pair.x] == null || projectedDots[pair.y] == null) continue;
            g.draw(new Line2D.Float(
                    projectedDots[pair.x].x,
                    SCREEN_HEIGHT - projectedDots[pair.x].y,// - because panel y starts from top
                    projectedDots[pair.y].x,
                    SCREEN_HEIGHT - projectedDots[pair.y].y));// - because panel y starts from top
        }
    }

    public Pair<Float>[] getProjectedDots(SimpleMove simpleMove) {
        Triple[] nodes = getNodes();
        Pair<Float>[] projectedDots = (Pair<Float>[]) new Pair<?>[nodes.length];
        int count =-1;
        for(Triple point: nodes) {
            Triple rotatedPoint = point.rotateXY(new Triple(x, y, z), rotation);
            count++;
            Pair<Float> projected = projectWithStrategy(simpleMove, point, rotatedPoint);
            if(projected == null) continue;
            projectedDots[count] = projected;
        }
        return projectedDots;
    }

    protected abstract Triple[] getNodes();

    protected abstract Pair<Float> projectWithStrategy(SimpleMove simpleMove, Triple point, Triple rotatedPoint);

}
