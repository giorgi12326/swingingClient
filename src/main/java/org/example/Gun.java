package org.example;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import static org.example.SimpleMove.SCREEN_HEIGHT;

public class Gun {
    public static List<Pair<Integer>> edges =  new ArrayList<>();

    float x;
    float y;
    float z;

    Triple[] nodes;

    public Gun(float x, float z, float y) {
        this.x = x;
        this.z = z;
        this.y = y;
        nodes = getNodes();
    }
    static{
        edges.add(new Pair<>(0,1));
        edges.add(new Pair<>(1,2));
        edges.add(new Pair<>(2,3));

        edges.add(new Pair<>(4,5));
        edges.add(new Pair<>(5,6));
        edges.add(new Pair<>(6,7));

        edges.add(new Pair<>(0,4));
        edges.add(new Pair<>(1,5));
        edges.add(new Pair<>(2,6));
        edges.add(new Pair<>(3,7));

        edges.add(new Pair<>(4,8));
        edges.add(new Pair<>(5,9));
        edges.add(new Pair<>(6,10));
        edges.add(new Pair<>(7,11));

        edges.add(new Pair<>(8,12));
        edges.add(new Pair<>(9,13));
        edges.add(new Pair<>(10,14));
        edges.add(new Pair<>(11,15));

        edges.add(new Pair<>(12,13));
        edges.add(new Pair<>(13,14));
        edges.add(new Pair<>(14,15));
        edges.add(new Pair<>(15,12));

        edges.add(new Pair<>(8,9));

    }
    public Triple[] getNodes() {
        return new Triple[]{
            new Triple(x + 0.04f,y + 0.04f, z + 0.1f),
            new Triple(x - 0.04f,y + 0.04f,z + 0.1f),
            new Triple(x - 0.04f,y - 0.04f,z + 0.1f),
            new Triple(x + 0.04f,y - 0.04f, z + 0.1f),

            new Triple(x + 0.02f,y + 0.02f, z + 0f),
            new Triple(x - 0.02f,y + 0.02f,z + 0f),
            new Triple(x - 0.02f,y - 0.02f,z + 0f),
            new Triple(x + 0.02f,y - 0.02f, z + 0f),

            new Triple(x + 0.01f,y + 0.01f, z -0.2f),
            new Triple(x -0.01f,y + 0.01f, z -0.2f),
            new Triple(x -0.01f,y - 0.01f, z -0.18f),
            new Triple(x + 0.01f,y - 0.01f, z-0.18f),

            new Triple(x + 0.01f,y - 0.1f, z-0.2f),
            new Triple(x -0.01f,y -0.1f, z-0.2f),
            new Triple(x -0.01f,y - 0.1f, z-0.18f),
            new Triple(x + 0.01f,y - 0.1f, z-0.18f),
        };
    }
    Pair<Float>[] getProjectedDotsForGun(SimpleMove simpleMove) {
        Pair<Float>[] projectedDots = (Pair<Float>[]) new Pair<?>[nodes.length];
        int count =-1;
        for(Triple point: getNodes()) {
            count++;
            Pair<Float> projected = simpleMove.projectTo2DWithoutRotatingAgainstCamera(point.x, point.y, point.z);
            if(projected == null) continue;
            projectedDots[count] = projected;
        }
        return projectedDots;
    }

    public void draw(Graphics2D g, SimpleMove simpleMove) {
        Pair<Float>[] projectedDotsForGun = getProjectedDotsForGun(simpleMove);

        for(Pair<Integer> pair : Gun.edges){
            if(projectedDotsForGun[pair.x] == null || projectedDotsForGun[pair.y] == null) continue;
            g.draw(new Line2D.Float(
                    projectedDotsForGun[pair.x].x,
                    SCREEN_HEIGHT - projectedDotsForGun[pair.x].y,// - because panel y starts from top
                    projectedDotsForGun[pair.y].x,
                    SCREEN_HEIGHT - projectedDotsForGun[pair.y].y));// - because panel y starts from top
        }
    }
}
