package org.example;

public class Ray {
    Triple position;
    Pair<Float> direction;
    float size;

    public Ray(Triple position, Pair<Float> direction, float size) {
        this.position = position;
        this.direction = direction;
        this.size = size;
    }
}
