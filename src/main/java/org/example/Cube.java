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
                    arr[count++] = new Triple(x + i*size/2f, y + j*size/2f, z + k*size/2f);
                }
            }
        }
        return arr;
    }

    public void update() {
//        rotateY(0.1f);
    }

    public void rotateY(float angle){
        float sin = (float)Math.sin(angle);
        float cos = (float)Math.cos(angle);

        for(Triple p : nodes){
            float x = p.x;
            float z = p.z;

            p.x = x * cos - z * sin;
            p.z = x * sin + z * cos;
        }
    }


    Pair<Float>[] getProjectedDotsForCube() {
        Pair<Float>[] projectedDots = (Pair<Float>[]) new Pair<?>[8];
        int count =-1;
        for(Triple point: getPoints()) {
            count++;
            Pair<Float> projected = UtilProject.projectTo2D(point.x, point.y, point.z);
            if(projected == null) continue;
            projectedDots[count] = projected;
        }
        return projectedDots;
    }

    public void draw(Graphics2D g) {
        Pair<Float>[] projectedDots = getProjectedDotsForCube();
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
