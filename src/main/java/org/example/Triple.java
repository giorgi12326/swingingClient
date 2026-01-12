package org.example;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static org.example.SimpleMove.SCREEN_HEIGHT;

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

    public Triple(Triple position) {
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    public Triple add(Triple other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;

        return this;
    }

    public Triple sub(Triple other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
        return this;
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
        if (projected == null) return;

        rect.x = projected.x;
        rect.y = SimpleMove.SCREEN_HEIGHT - projected.y;
        rect.width = 5f;
        rect.height = 5f;

        g.fill(rect);
    }

    public Triple normalize() {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0) return new Triple(0f, 0f, 0f); // avoid divide by zero
        return new Triple(x / len, y / len, z / len);
    }
    
    public Triple scale(float scale){
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;
        return this;
    }
    
    public Triple cross(Triple other) {
        this.x = this.y * other.z - this.z * other.y;
        this.y = this.z * other.x - this.x * other.z;
        this.z = this.x * other.y - this.y * other.x;
        return this;
    }
    
    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
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


