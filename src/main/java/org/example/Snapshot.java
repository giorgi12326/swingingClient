package org.example;

import java.util.ArrayList;
import java.util.List;

public class Snapshot {
    long time;

    BulletSnapshot[] bullets = new BulletSnapshot[500];
    public int bulletSize;

    Client me = new Client();
    Client[] clients = new Client[4];

    final Object mutex =  new Object();

    public Snapshot() {
        // initialize bullets
        for (int i = 0; i < bullets.length; i++) {
            bullets[i] = new BulletSnapshot();
        }

        // initialize clients
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new Client();
        }
    }


}
