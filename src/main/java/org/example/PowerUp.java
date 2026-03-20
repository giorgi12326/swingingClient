package org.example;

import java.util.ArrayList;
import java.util.List;

public class PowerUp extends Projectable{
    int type;
    float rotationX;
    float rotationY = (float) Math.random();

    public static List<Pair<Integer>> edges =  new ArrayList<>();

    static {
        int segments = 12;

        // front circle
        for (int i = 0; i < segments; i++) {
            edges.add(new Pair<>(i, (i + 1) % segments));
        }

        // back circle
        for (int i = 0; i < segments; i++) {
            edges.add(new Pair<>(i + segments, ((i + 1) % segments) + segments));
        }

        // connect front ↔ back
        for (int i = 0; i < segments; i++) {
            edges.add(new Pair<>(i, i + segments));
        }
    }

    @Override
    public Triple[] getNodes() {
        int segments = 12;
        float radius = 0.2f;
        float thickness = 0.01f;
        rotationY += 0.01f;

        Triple[] nodes = new Triple[24];

        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;

            float yPos = y + (float)(radius * Math.cos(angle));
            float zPos = z + (float)(radius * Math.sin(angle));

            // front circle
            nodes[i] = new Triple(x - thickness, yPos, zPos).rotateXY(new Triple(x,y,z), new Pair<>(rotationX, rotationY));

            // back circle
            nodes[i + segments] =  new Triple(x + thickness, yPos, zPos).rotateXY(new Triple(x,y,z), new Pair<>(rotationX, rotationY));
        }

        return nodes;
    }

    @Override
    public List<Pair<Integer>> getStaticEdges() {
        return edges;
    }

    @Override
    protected Pair<Float> projectWithStrategy(Triple point, Triple rotatedPoint) {
        return UtilProject.projectTo2D(point.x, point.y, point.z);
    }
}
