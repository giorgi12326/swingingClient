package org.example;

import java.util.ArrayList;
import java.util.List;

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

    float lx;
    float ly;
    float lz;

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
                    arr[count++] = new Triple(x + lx + i*0.5f, y + ly + j*0.5f, z + lz + k*0.5f);
                }
            }
        }
        return arr;
    }

    public void update() {
//        rotateY(0.01f);
    }

    public void rotateY(float angle) {

        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);

        float newY = ly * c - lx * s;
        float newX = ly * s + lx * c;

        ly = newY;
        lx = newX;
    }

}
