package org.example;

import java.net.InetAddress;

public class Client {
    Triple sum = new Triple(0f,0f,0f);
    Triple cameraCoords = new Triple(0f,0f,0f);
    Pair<Float> cameraRotation = new Pair<>(0f,0f);
    BulletHead heldBullet = new BulletHead();
    GrapplingHead grapplingHead = new GrapplingHead(0,0f,0);
    boolean swinging;
    boolean grapplingEquipped;
    boolean inAir;
    boolean bulletHeld;

    Cube hitbox = new Cube(0f,0f,0f,0.5f);

}
