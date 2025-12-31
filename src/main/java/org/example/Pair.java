package org.example;

public class Pair<T> {
    T x;
    T y;

    public Pair(T x, T y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
