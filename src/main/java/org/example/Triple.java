package org.example;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static org.example.SimpleMove.SCREEN_HEIGHT;

public class Triple {
    float x;
    float y;
    float z;
    
    public Triple(Float x, Float y, Float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Triple(Triple position) {
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    public void add(Triple other) {
        x += other.x;
        y += other.y;
        z += other.z;
    }

    @Override
    public String toString() {
        return "Triple{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    public void draw(Graphics2D g, SimpleMove simpleMove) {
        Pair<Float> projected = simpleMove.projectTo2D(x, y, z);
        if(projected == null) return;
        g.fill(new Rectangle2D.Float(projected.x, SCREEN_HEIGHT - projected.y, 5f, 5f));
    }
}
