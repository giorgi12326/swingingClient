package org.example;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

import static org.example.SimpleMove.*;

public class SnapShotReceiver implements Runnable {
    private final DatagramSocket socket;

    public SnapShotReceiver(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try {
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            ByteBuffer bb = ByteBuffer.wrap(buffer);

            while (true) {
                socket.receive(packet);

                System.out.println(System.currentTimeMillis() - lastPacketReceived);
                lastPacketReceived = System.currentTimeMillis();

                Snapshot snapshot = snapshots[(SimpleMove.snapshotPointer++)%snapshots.length];
                bb.position(0);
                bb.limit(packet.getLength());

                snapshot.me.cameraCoords.x = bb.getFloat();
                snapshot.me.cameraCoords.y = bb.getFloat();
                snapshot.me.cameraCoords.z = bb.getFloat();
                snapshot.me.health = bb.getInt();

                snapshot.bulletSize = bb.getInt();

                for (BulletHead head : bulletsPool) head.x = 0;

                for (int i = 0; i < snapshot.bulletSize; i++) {
                    BulletSnapshot bulletSnapshot = snapshot.bullets[i];
                    bulletSnapshot.x = bb.getFloat();
                    bulletSnapshot.y = bb.getFloat();
                    bulletSnapshot.z = bb.getFloat();
                    bulletSnapshot.rotationX = bb.getFloat();
                    bulletSnapshot.rotationY = bb.getFloat();
                    bulletSnapshot.shot = bb.get() == 1;
                    bulletSnapshot.flying = bb.get() == 1;
                }

                snapshot.me.bulletHeld = (bb.get() == 1);
                snapshot.me.grapplingEquipped = (bb.get() == 1);
                snapshot.me.grapplingHead.shot = (bb.get() == 1);
                snapshot.me.grapplingHead.flying = (bb.get() == 1);

                snapshot.me.grapplingHead.x = bb.getFloat();
                snapshot.me.grapplingHead.y = bb.getFloat();
                snapshot.me.grapplingHead.z = bb.getFloat();
                snapshot.me.grapplingHead.rotation.x = bb.getFloat();
                snapshot.me.grapplingHead.rotation.y = bb.getFloat();

                int powerUpSize = bb.getInt();
                snapshot.me.powerUps.clear();
                for (int j = 0; j < powerUpSize; j++) {
                    snapshot.me.powerUps.add(bb.getInt());
                }

                snapshot.clientSize = bb.getInt();
                for (int i = 0; i < snapshot.clientSize; i++) {
                    Client client = snapshot.clients[i];
                    client.cameraCoords.x = bb.getFloat();
                    client.cameraCoords.y = bb.getFloat();
                    client.cameraCoords.z = bb.getFloat();
                    client.cameraRotation.x = bb.getFloat();
                    client.cameraRotation.y = bb.getFloat();

                    client.grapplingHead.x = bb.getFloat();
                    client.grapplingHead.y = bb.getFloat();
                    client.grapplingHead.z = bb.getFloat();
                    client.grapplingHead.rotation.x = bb.getFloat();
                    client.grapplingHead.rotation.y = bb.getFloat();
                    client.grapplingEquipped = bb.get() == 1;
                }

                snapshot.powerUpSize = bb.getInt();
                for (PowerUp powerUp : powerUpPool) powerUp.z = 0;

                for (int i = 0; i < snapshot.powerUpSize; i++) {
                    PowerUpSnapShot powerUpSnapShot = snapshot.powerUps[i];
                    powerUpSnapShot.z = 0;
                    powerUpSnapShot.x = bb.getFloat();
                    powerUpSnapShot.y = bb.getFloat();
                    powerUpSnapShot.z = bb.getFloat();
                    powerUpSnapShot.type = bb.getInt();
                }

                snapshot.me.time = bb.getLong();
                snapshot.time = bb.getLong();

                currentPing = System.currentTimeMillis() - snapshot.me.time;

                lastRecievedServerTime = snapshot.time;

                serverOffset = Math.min(serverOffset, snapshot.time - System.currentTimeMillis());

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
