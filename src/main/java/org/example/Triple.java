package org.example;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Triple {
    float x;
    float y;
    float z;

    private final Rectangle2D.Float rect = new Rectangle2D.Float();

    public Triple(Float x, Float y, Float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "Triple{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    public void draw(Graphics2D g) {
        Pair<Float> projected = UtilProject.projectTo2D(x, y, z);
        if (projected == null) return;

        rect.x = projected.x;
        rect.y = SimpleMove.SCREEN_HEIGHT - projected.y;
        rect.width = 5f;
        rect.height = 5f;

        g.fill(rect);
    }

    public Triple rotateXY(Triple center, Pair<Float> rotation) {
        float px = this.x - center.x;
        float py = this.y - center.y;
        float pz = this.z - center.z;

        float cosX = (float) Math.cos(-rotation.x);
        float sinX = (float) Math.sin(-rotation.x);
        float py2 = py * cosX - pz * sinX;
        float pz2 = py * sinX + pz * cosX;

        float cosY = (float) Math.cos(-rotation.y);
        float sinY = (float) Math.sin(-rotation.y);
        float px2 = px * cosY - pz2 * sinY;
        float pz3 = px * sinY + pz2 * cosY;

        this.x = px2 + center.x;
        this.y = py2 + center.y;
        this.z = pz3 + center.z;

        return this;
    }
}


