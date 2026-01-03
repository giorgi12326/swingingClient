package org.example;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import static org.example.SimpleMove.SCREEN_HEIGHT;

public class Cube {
    public static List<Pair<Integer>> edges =  new ArrayList<>();

    static{
        edges.add(new Pair<>(0,1));
        edges.add(new Pair<>(1,3));
        edges.add(new Pair<>(2,0));
        edges.add(new Pair<>(2,3));

        edges.add(new Pair<>(4,5));
        edges.add(new Pair<>(5,7));
        edges.add(new Pair<>(6,4));
        edges.add(new Pair<>(6,7));

        edges.add(new Pair<>(0,4));
        edges.add(new Pair<>(1,5));
        edges.add(new Pair<>(2,6));
        edges.add(new Pair<>(3,7));
    }

    float x;
    float y;
    float z;

    float size;

    Triple[] nodes;

    public Cube(float x, float y, float z, float size) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.size = size;
        nodes = getPoints();
    }

    public Triple[] getPoints(){
        Triple[] arr = new Triple[8];

        int count = 0;
        for (int i = -1; i < 2; i+=2) {
            for (int j = -1; j < 2; j+=2) {
                for (int k = -1; k < 2; k+=2) {
                    arr[count++] = new Triple(x + i*0.5f, y + j*0.5f, z + k*0.5f);
                }
            }
        }
        return arr;
    }

    public void update() {
//        rotateY(0.01f);
    }

    Pair<Float>[] getProjectedDotsForCube(SimpleMove simpleMove) {
        Pair<Float>[] projectedDots = (Pair<Float>[]) new Pair<?>[8];
        int count =-1;
        for(Triple point: getPoints()) {
            count++;
            Pair<Float> projected = simpleMove.projectTo2D(point.x, point.y, point.z);
            if(projected == null) continue;
            projectedDots[count] = projected;
        }
        return projectedDots;
    }

    public boolean isPointInCube(Triple position){
        return Math.abs(x - position.x) <= size / 2 &&
                Math.abs(y - position.y) <= size / 2 &&
                Math.abs(z - position.z) <= size / 2;
    }

    public void draw(Graphics2D g, SimpleMove simpleMove) {
        Pair<Float>[] projectedDots = getProjectedDotsForCube(simpleMove);
        for(Pair<Integer> pair : Cube.edges){
            if(projectedDots[pair.x] == null || projectedDots[pair.y] == null) continue;
            g.draw(new Line2D.Float(
                    projectedDots[pair.x].x,
                    SCREEN_HEIGHT - projectedDots[pair.x].y,// - because panel y starts from top
                    projectedDots[pair.y].x,
                    SCREEN_HEIGHT - projectedDots[pair.y].y));// - because panel y starts from top
        }
    }
}
