package org.example;


import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Frame;
import java.awt.Color;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class SimpleMove extends Canvas {
    public static final float SCREEN_WIDTH = 1280f;
    public static final float SCREEN_HEIGHT = 720f;
    public static boolean hit = false;
    public static float deltaTime = 1f;
    public static float FOV = 1;
    long lastTime = System.nanoTime();
    public static long lastPacketReceived = 0;

    public static long serverOffset = Long.MAX_VALUE;

    static int INTERP_DELAY_MS = 30;

    public static boolean swinging = false;
    public static boolean grapplingEquipped = false;
    int health = 100;

    public static Triple cameraCoords = new Triple(0f,0f,0f);
    public static Pair<Float> cameraRotation = new Pair<>(0f,0f);

    public static boolean[] keysPressed = new boolean[256];
    public static boolean[] buttonsPressed = new boolean[256];

    int[] trackedKeys = {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_SPACE, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3};
    int[] trackedButtons = {MouseEvent.BUTTON1, MouseEvent.BUTTON3};

    List<Cube> cubes;
    List<Triple> floor = new ArrayList<>();
    BulletHead heldBullet = new BulletHead();
    boolean bulletHeld;

    Client[] clients = new Client[4];
    public static BulletHead[] bulletsPool = new BulletHead[500];
    public static PowerUp[] powerUpPool = new PowerUp[100];
    public static Snapshot[] snapshots = new Snapshot[30];
    public static int snapshotPointer = 0;

    Gun gun = new Gun(0,0,0);
    GrapplingHead grapplingHead = new GrapplingHead(0,0f,0);
    public static long currentPing = 0;

    byte[] sendArray = new byte[26];
    ByteBuffer sendBuffer = ByteBuffer.wrap(sendArray);
    DatagramPacket sendPacket;

    DatagramSocket socket = null;
    private int clientSize = 0;
    public static long lastRecievedServerTime;
    private boolean isDead;
    private long timeOfLocalDeath;
    private List<Integer> givenPowerups = new ArrayList<>();

    public SimpleMove() {
        this.cubes = new ArrayList<>();
        cubes.add(new Cube(0.5f,0.5f, 3.5f,1f));
        cubes.add(new Cube(0.5f,1.5f, 5.5f,1f));
        cubes.add(new Cube(0.5f,2.5f, 6.5f,1f));
        cubes.add(new Cube(0.5f,3.5f, 9.5f,1f));
        cubes.add(new Cube(0.5f,3.5f, 12.5f,1f));
        cubes.add(new Cube(0.5f,3.5f, 15.5f,1f));
        cubes.add(new Cube(0.5f,4.5f, 18.5f,1f));
        cubes.add(new Cube(0.5f,4.5f, 23.5f,1f));
        cubes.add(new Cube(0.5f,4.5f, 30.5f,1f));
        new Controller(this);

        setSize((int)SCREEN_WIDTH, (int)SCREEN_HEIGHT);

        for (int i = 0; i < 500; i++)
            bulletsPool[i] = new BulletHead();

        for (int i = 0; i < 100; i++)
            powerUpPool[i] = new PowerUp();

        for (int i = 0; i < 30; i++)
            snapshots[i] = new Snapshot();

        for (int i = 0; i < 4; i++)
            clients[i] = new Client();

        for (int i = -10; i < 10; i++)
            for (int j = -10; j < 10; j++)
                floor.add(new Triple(i*1f, 0f, j*1f));
        try {
//            sendPacket = new DatagramPacket(sendArray, sendArray.length, InetAddress.getByName("82.211.163.67"), 1234);
            sendPacket = new DatagramPacket(sendArray, sendArray.length, InetAddress.getLocalHost(), 1234);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        try {
            socket = new DatagramSocket(1945);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        new Thread(new SnapShotReceiver(socket)).start();

        updateAndSend();

    }

    public void updateAndSend(){
        try {
            createBufferStrategy(2);   // double buffering
            BufferStrategy bs = getBufferStrategy();

            while (true) {
                long now = System.nanoTime();
                deltaTime = (now - lastTime) / 1_000_000_000f;
                lastTime = now;

                processReadings();
                render(bs);
                sendInputs();

                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private void processReadings() {
        Snapshot older = null;
        Snapshot newer = null;

        long renderTime = lastRecievedServerTime + currentPing/2 - INTERP_DELAY_MS;

        for (Snapshot s : snapshots) {
            if (s.time <= renderTime)
                if (older == null || s.time > older.time)
                    older = s;
            if (s.time > renderTime)
                if (newer == null || s.time  < newer.time)
                    newer = s;
        }

        if (older != null && newer != null) {
            synchronized (older.mutex) {
                synchronized (newer.mutex) {
                    clientSize = Math.min(older.clientSize, newer.clientSize);

                    float t = (renderTime - older.time) / (float) (newer.time - older.time);

                    cameraCoords.x = lerp(older.me.cameraCoords.x, newer.me.cameraCoords.x, t);
                    cameraCoords.y = lerp(older.me.cameraCoords.y, newer.me.cameraCoords.y, t);
                    cameraCoords.z = lerp(older.me.cameraCoords.z, newer.me.cameraCoords.z, t);
                    health = (int) lerp(older.me.health, newer.me.health, t);

                    bulletHeld = older.me.bulletHeld && newer.me.bulletHeld;
                    grapplingEquipped = older.me.grapplingEquipped && newer.me.grapplingEquipped;
                    grapplingHead.shot = older.me.grapplingHead.shot && newer.me.grapplingHead.shot;
                    grapplingHead.flying = older.me.grapplingHead.flying && newer.me.grapplingHead.flying;

                    grapplingHead.x = lerp(older.me.grapplingHead.x, newer.me.grapplingHead.x, t);
                    grapplingHead.y = lerp(older.me.grapplingHead.y, newer.me.grapplingHead.y, t);
                    grapplingHead.z = lerp(older.me.grapplingHead.z, newer.me.grapplingHead.z, t);
                    grapplingHead.rotation.x = lerp(older.me.grapplingHead.rotation.x, newer.me.grapplingHead.rotation.x, t);
                    grapplingHead.rotation.y = lerp(older.me.grapplingHead.rotation.y, newer.me.grapplingHead.rotation.y, t);

                    for (int i = 0; i < Math.min(older.bulletSize, newer.bulletSize); i++) {
                        BulletSnapshot ob = older.bullets[i];
                        BulletSnapshot nb = newer.bullets[i];
                        bulletsPool[i].x = lerp(ob.x, nb.x, t);
                        bulletsPool[i].y = lerp(ob.y, nb.y, t);
                        bulletsPool[i].z = lerp(ob.z, nb.z, t);
                        bulletsPool[i].rotation.x = lerp(ob.rotationX, nb.rotationX, t);
                        bulletsPool[i].rotation.y = lerp(ob.rotationY, nb.rotationY, t);
                        bulletsPool[i].shot = ob.shot && nb.shot;
                        bulletsPool[i].flying = ob.flying && nb.flying;
                    }

                    for (int i = 0; i < Math.min(older.clientSize, newer.clientSize); i++) {
                        Client oc = older.clients[i];
                        Client nc = newer.clients[i];

                        clients[i].cameraCoords.x = lerp(oc.cameraCoords.x, nc.cameraCoords.x, t);
                        clients[i].cameraCoords.y = lerp(oc.cameraCoords.y, nc.cameraCoords.y, t);
                        clients[i].cameraCoords.z = lerp(oc.cameraCoords.z, nc.cameraCoords.z, t);
                        clients[i].cameraRotation.x = lerp(oc.cameraRotation.x, nc.cameraRotation.x, t);
                        clients[i].cameraRotation.y = lerp(oc.cameraRotation.y, nc.cameraRotation.y, t);

                        clients[i].grapplingHead.x = lerp(oc.grapplingHead.x, nc.grapplingHead.x, t);
                        clients[i].grapplingHead.y = lerp(oc.grapplingHead.y, nc.grapplingHead.y, t);
                        clients[i].grapplingHead.z = lerp(oc.grapplingHead.z, nc.grapplingHead.z, t);
                        clients[i].grapplingHead.rotation.x = lerp(oc.grapplingHead.rotation.x, nc.grapplingHead.rotation.x, t);
                        clients[i].grapplingHead.rotation.y = lerp(oc.grapplingHead.rotation.y, nc.grapplingHead.rotation.y, t);

                        clients[i].hitbox.x = clients[i].cameraCoords.x;
                        clients[i].hitbox.y = clients[i].cameraCoords.y + 0.25f;
                        clients[i].hitbox.z = clients[i].cameraCoords.z;
                    }

                    givenPowerups.clear();
                    givenPowerups.addAll(newer.me.powerUps);

                    for (int i = 0; i < Math.min(older.powerUpSize, newer.powerUpSize); i++) {
                        PowerUpSnapShot ob = older.powerUps[i];
                        PowerUpSnapShot nb = newer.powerUps[i];
                        powerUpPool[i].x = lerp(ob.x, nb.x, t);
                        powerUpPool[i].y = lerp(ob.y, nb.y, t);
                        powerUpPool[i].z = lerp(ob.z, nb.z, t);
                        powerUpPool[i].type = nb.type;
                    }
                }
            }
        }

        gun.x = cameraCoords.x + 0.1f;
        gun.y = cameraCoords.y;
        gun.z = cameraCoords.z + 0.3f;

        if(!grapplingHead.shot){
            grapplingHead.x = cameraCoords.x + 0.1f;
            grapplingHead.y = cameraCoords.y;
            grapplingHead.z = cameraCoords.z + 1f;
            grapplingHead.rotation.x = 0f;
            grapplingHead.rotation.y = 0f;
        }

        heldBullet.x = cameraCoords.x + 0.3f;
        heldBullet.y = cameraCoords.y;
        heldBullet.z = cameraCoords.z + 0.8f;

        if(health <= 0 && !isDead) {
            timeOfLocalDeath = System.currentTimeMillis();
            isDead = true;
        }

        if(isDead && System.currentTimeMillis() - timeOfLocalDeath > 5000) {
            isDead = false;
        }
    }

    private void sendInputs() throws IOException {
        sendBuffer.clear();

        for (int trackedKey : trackedKeys) {
            sendBuffer.put((byte) (keysPressed[trackedKey] ? 1 : 0));
        }
        for (int trackedButton : trackedButtons) {
            sendBuffer.put((byte) (buttonsPressed[trackedButton] ? 1 : 0));
        }

        sendBuffer.putFloat(cameraRotation.x);
        sendBuffer.putFloat(cameraRotation.y);
        sendBuffer.putLong(System.currentTimeMillis());

        sendPacket.setLength(sendBuffer.position());
        socket.send(sendPacket);
    }

    public float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private void render(BufferStrategy bs) {
        Graphics gj = bs.getDrawGraphics();
        Graphics2D g = (Graphics2D) gj;

        clearScreen(g);

        drawCrosshair(g);

        for (int i = 0; i < clientSize; i++) {
            Client client = clients[i];
            g.setColor(Color.PINK);
//            client.hitbox.rotateY(1);
            client.hitbox.draw(g);
        }
        g.setColor(Color.BLUE);

        for(Triple point : floor)
            point.draw(g);

        for (Cube cube: cubes)
            cube.draw(g);

        g.setColor(Color.YELLOW);
        gun.draw(g);

        if(grapplingEquipped) {
            grapplingHead.draw(g);

            Pair<Float>[] projectedDotsForGun = gun.getProjectedDots();

            Pair<Float>[] hookProjected = grapplingHead.getProjectedDots();
            if(projectedDotsForGun[Gun.edges.get(12).x] != null && hookProjected[GrapplingHead.edges.get(16).y] != null)
                g.draw(new Line2D.Float(
                        projectedDotsForGun[Gun.edges.get(12).x].x,
                        SCREEN_HEIGHT -  projectedDotsForGun[Gun.edges.get(12).x].y,// - because panel y starts from top
                        hookProjected[GrapplingHead.edges.get(16).y].x,
                        SCREEN_HEIGHT - hookProjected[GrapplingHead.edges.get(16).y].y));// - because panel y starts from top
        }

        if((bulletHeld && !grapplingEquipped) || (grapplingEquipped && bulletHeld && grapplingHead.shot))
            heldBullet.draw(g);

        for (BulletHead bullet : bulletsPool)
            if(bullet.x != 0)
                bullet.draw(g);

        for (PowerUp powerUp: powerUpPool)
            if(powerUp.z != 0) {
                if (powerUp.type == 1)
                    g.setColor(Color.lightGray);
                if (powerUp.type == 2)
                    g.setColor(Color.RED);
                powerUp.draw(g);
            }

        for (int i = 0; i < givenPowerups.size(); i++) {
            int type = givenPowerups.get(i);
            if (type == 1)
                g.setColor(Color.lightGray);
            if (type == 2)
                g.setColor(Color.RED);

            g.drawOval( 30, (int) (SCREEN_HEIGHT - (i+1) * 50), 30, 30);
        }
        g.setColor(Color.yellow);


        g.drawString("FPS: " + (int)(1/deltaTime), 30, 30);
        g.drawString("delay: " + INTERP_DELAY_MS, 80, 30);
        g.drawString(String.valueOf(health), SCREEN_WIDTH/2 - 5 , 30 );
        g.drawString("ping: " + currentPing, SCREEN_WIDTH - 100, 30);


        if(isDead){
            g.drawString("Respawn in: " + (5 - (System.currentTimeMillis() - timeOfLocalDeath)/1000), SCREEN_WIDTH/2 - 20, SCREEN_HEIGHT/2f);
        }

        g.dispose();
        bs.show();
    }

    private void clearScreen(Graphics2D g) {
        if(hit)
            g.setColor(Color.RED);
        else
            g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawCrosshair(Graphics2D g) {
        g.setColor(Color.lightGray);
        g.fill(new Rectangle2D.Float(SCREEN_WIDTH/2f - 10f, SCREEN_HEIGHT/2f-2f, 20f, 4f));
        g.fill(new Rectangle2D.Float(SCREEN_WIDTH/2f - 2f, SCREEN_HEIGHT/2f-10f, 4f, 20f));
    }

    public static void main(String[] args) {
        createWindow();
    }

    private static void createWindow() {
        Frame frame = new Frame("Simple Moving Rectangle");
        SimpleMove canvas = new SimpleMove();
        frame.add(canvas);
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        canvas.start();
    }

}
