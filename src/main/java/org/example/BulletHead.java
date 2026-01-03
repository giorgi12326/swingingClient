package org.example;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import static org.example.SimpleMove.*;

public class BulletHead {
    public static List<Pair<Integer>> edges =  new ArrayList<>();

    float x;
    float y;
    float z;

    Pair<Float> rotation = new Pair<>(0f,0f);

    boolean shot;
    boolean flying;

    Triple direction;

    Triple[] nodes;

    float r = 0.3f;

    public BulletHead() {
        nodes = getNodes();
    }

    static{
        edges.add(new Pair<>(0,1));
        edges.add(new Pair<>(1,2));
        edges.add(new Pair<>(2,3));
        edges.add(new Pair<>(3,0));

        edges.add(new Pair<>(4,5));
        edges.add(new Pair<>(5,6));
        edges.add(new Pair<>(6,7));
        edges.add(new Pair<>(7,4));

        edges.add(new Pair<>(0,4));
        edges.add(new Pair<>(1,5));
        edges.add(new Pair<>(2,6));
        edges.add(new Pair<>(3,7));

        edges.add(new Pair<>(4,8));
        edges.add(new Pair<>(5,8));
        edges.add(new Pair<>(6,8));
        edges.add(new Pair<>(7,8));



    }
    public Triple[] getNodes() {
        float dx = (float) Math.cos(Math.PI / 6) * r;
        float dy = (float) Math.sin(Math.PI / 6) * r;
        return new Triple[]{
                new Triple(x + dx + 0.04f,y + dy + 0.04f, z),
                new Triple(x + dx - 0.04f,y + dy + 0.04f, z),
                new Triple(x + dx - 0.04f,y + dy - 0.04f, z),
                new Triple(x + dx + 0.04f,y + dy - 0.04f, z),

                new Triple(x + dx + 0.04f,y + dy + 0.04f, z + 0.16f),
                new Triple(x + dx - 0.04f,y + dy + 0.04f, z + 0.16f),
                new Triple(x + dx - 0.04f,y + dy - 0.04f, z + 0.16f),
                new Triple(x + dx + 0.04f,y + dy - 0.04f, z + 0.16f),

                new Triple(x + dx,y + dy, z + 0.2f),
        };
    }

    Pair<Float>[] getProjectedDotsForGun(SimpleMove simpleMove) {
        Pair<Float>[] projectedDots = (Pair<Float>[]) new Pair<?>[nodes.length];
        int count =-1;
        for(Triple point: getNodes()) {
            Triple rotatedPoint = point.rotateXY(new Triple(x, y, z), rotation);
            count++;
            Pair<Float> projected;
            if(shot && flying)
                projected = simpleMove.projectTo2D(rotatedPoint.x, rotatedPoint.y, rotatedPoint.z);
            else if(shot)
                projected = simpleMove.projectTo2D(rotatedPoint.x, rotatedPoint.y, rotatedPoint.z);
            else
               projected = simpleMove.projectTo2DWithoutRotatingAgainstCamera(point.x, point.y, point.z);
            if(projected == null) continue;
            projectedDots[count] = projected;
        }
        return projectedDots;
    }

    public void drawEdges(Graphics2D g, SimpleMove simpleMove) {
        Pair<Float>[] projectedDotsForGun = getProjectedDotsForGun(simpleMove);

        for(Pair<Integer> pair : edges){
            if(projectedDotsForGun[pair.x] == null || projectedDotsForGun[pair.y] == null) continue;
            g.draw(new Line2D.Float(
                    projectedDotsForGun[pair.x].x,
                    SCREEN_HEIGHT - projectedDotsForGun[pair.x].y,// - because panel y starts from top
                    projectedDotsForGun[pair.y].x,
                    SCREEN_HEIGHT - projectedDotsForGun[pair.y].y));// - because panel y starts from top
        }

    }


}
