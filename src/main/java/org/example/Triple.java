package org.example;

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

    @Override
    public String toString() {
        return "Triple{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
