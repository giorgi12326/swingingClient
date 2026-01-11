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
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleMove extends Canvas implements Runnable, KeyListener, MouseListener, MouseMotionListener {
    public static final float SCREEN_WIDTH = 1280f;
    public static final float SCREEN_HEIGHT = 720f;
    public static final float moveSpeed = 10f;
    public static boolean hit = false;
    public static float deltaTime = 1f;
    public static float FOV = 1;
    long lastTime = System.nanoTime();
    long lastPacketReceived = 0;

    public static boolean swinging = false;
    public static boolean grapplingEquipped = false;

    public static Triple cameraCoords = new Triple(0f,0f,0f);
    public static Pair<Float> cameraRotation = new Pair<>(0f,0f);

    boolean[] keysPressed = new boolean[256];
    boolean[] buttonsPressed = new boolean[256];

    public final Set<Integer> keysDown = new HashSet<>();

    int[] trackedKeys = {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_SPACE};
    int[] trackedButtons = {MouseEvent.BUTTON1, MouseEvent.BUTTON3};

    List<Cube> cubes;
    List<DeathCube> deathCubes = new ArrayList<>();
    List<Triple> floor = new ArrayList<>();
    List<BulletHead> bulletsPool = new ArrayList<>();
    BulletHead heldBullet = new BulletHead();
    boolean bulletHeld;
    List<Client> clients = new ArrayList<>();

    Gun gun = new Gun(0,0,0);
    GrapplingHead grapplingHead = new GrapplingHead(0,0f,0);
    private Triple anchor;

    DatagramSocket socket = null;
    private Triple tempPosition = new Triple(0f,0f,0f);

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

        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);

        setSize((int)SCREEN_WIDTH, (int)SCREEN_HEIGHT);
        for (int i = 0; i < 50; i++) {
            bulletsPool.add(new BulletHead());
        }

        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                floor.add(new Triple(i*1f, 0f, j*1f));
            }
        }
    }

    public void start() {
        try {
            socket = new DatagramSocket(1234);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        new Thread(this).start();// just use run?

        new Thread(() -> {

            try {
                byte[] buffer = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());

                while (true) {

                    socket.receive(packet);

                    bb.position(0);
                    bb.limit(packet.getLength());

                    System.out.println("packet took " + (System.currentTimeMillis() - lastPacketReceived) + "ms");
                    lastPacketReceived = System.currentTimeMillis();


                    float x = bb.getFloat();
                    float y = bb.getFloat();
                    float z = bb.getFloat();
                    int bulletSize = bb.getInt();

                    for (BulletHead head : bulletsPool) head.x = 0;

                    for (int i = 0; i < bulletSize; i++) {
                        BulletHead bulletHead = bulletsPool.get(i);
                        bulletHead.x = bb.getFloat();
                        bulletHead.y = bb.getFloat();
                        bulletHead.z = bb.getFloat();
                        bulletHead.rotation.x = bb.getFloat();
                        bulletHead.rotation.y = bb.getFloat();
                        bulletHead.shot = bb.get() == 1;
                        bulletHead.flying = bb.get() == 1;
                    }

                    bulletHeld = (bb.get() == 1);
                    grapplingEquipped = (bb.get() == 1);

                    int clientSize = bb.getInt();
                    while(clientSize !=  clients.size()) {
                        if(clientSize > clients.size())
                            clients.add(new Client());
                        if(clientSize > clients.size())
                            clients.remove(clients.size()-1);
                    }
                    for (int i = 0; i < clientSize; i++) {
                        Client client = clients.get(i);
                        client.cameraCoords.x =  bb.getFloat();
                        client.cameraCoords.y =  bb.getFloat();
                        client.cameraCoords.z =  bb.getFloat();
                        client.cameraRotation.x =  bb.getFloat();
                        client.cameraRotation.y =  bb.getFloat();
                        client.grapplingEquipped =  bb.get()==1;
                    }

                    tempPosition.x = x;
                    tempPosition.y = y;
                    tempPosition.z = z;

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

    }

    @Override
    public void run(){
        try {
            createBufferStrategy(2);   // double buffering
            BufferStrategy bs = getBufferStrategy();

            while (true) {
                ByteBuffer buffer = ByteBuffer.allocate(15);
                for (int trackedKey : trackedKeys) {
                    buffer.put((byte) (keysPressed[trackedKey] ? 1 : 0));
                }
                for (int trackedKey : trackedButtons) {
                    buffer.put((byte) (buttonsPressed[trackedKey] ? 1 : 0));
                }

                buffer.putFloat(cameraRotation.x);
                buffer.putFloat(cameraRotation.y);

                byte[] data = buffer.array();

                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("82.211.163.67"), 1234);
                socket.send(packet);

                long now = System.nanoTime();
                deltaTime = (now - lastTime) / 1_000_000_000f;
                lastTime = now;

                update();
                render(bs);

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

    private void update() {
        cameraCoords.x = tempPosition.x;
        cameraCoords.y = tempPosition.y;
        cameraCoords.z = tempPosition.z;

        gun.x = cameraCoords.x + 0.1f;
        gun.y = cameraCoords.y;
        gun.z = cameraCoords.z + 0.3f;

        heldBullet.x = cameraCoords.x;
        heldBullet.y = cameraCoords.y- 0.15f;
        heldBullet.z = cameraCoords.z + 0.8f;

    }

    private void render(BufferStrategy bs) {
        Graphics gj = bs.getDrawGraphics();
        Graphics2D g = (Graphics2D) gj;

        clearScreen(g);

        drawCrosshair(g);

        g.setColor(Color.BLUE);

        for(Client client : clients) {
            client.hitbox.x = client.cameraCoords.x;
            client.hitbox.y = client.cameraCoords.y;
            client.hitbox.z = client.cameraCoords.z;
//            client.hitbox.update();
            client.hitbox.draw(g, this);
        }

        for(Triple point : floor)
            point.draw(g,this);

        for (Cube cube: cubes)
            cube.draw(g, this);

        for (DeathCube cube: deathCubes)
            cube.draw(g, this);

        g.setColor(Color.YELLOW);
        gun.draw(g, this);


        if(grapplingEquipped) {
            grapplingHead.drawEdges(g, this);

            Pair<Float>[] projectedDotsForGun = gun.getProjectedDots(this);

            Pair<Float>[] hookProjected = grapplingHead.getProjectedDots(this);
            if(projectedDotsForGun[Gun.edges.get(12).x] != null && hookProjected[GrapplingHead.edges.get(16).y] != null)
                g.draw(new Line2D.Float(
                        projectedDotsForGun[Gun.edges.get(12).x].x,
                        SCREEN_HEIGHT -  projectedDotsForGun[Gun.edges.get(12).x].y,// - because panel y starts from top
                        hookProjected[GrapplingHead.edges.get(16).y].x,
                        SCREEN_HEIGHT - hookProjected[GrapplingHead.edges.get(16).y].y));// - because panel y starts from top

        }
        if(bulletHeld)
            heldBullet.drawEdges(g, this);

        for (BulletHead bullet : bulletsPool){
            if(bullet.x != 0)
                bullet.drawEdges(g, this);
        }

        g.drawString("FPS: " + (int)(1/deltaTime), 30, 30);

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

    private Triple rotationToDirection(Pair<Float> rotation) {
        float dx = (float)(Math.cos(rotation.x) * Math.sin(rotation.y));
        float dy = (float)(Math.sin(rotation.x));
        float dz = (float)(Math.cos(rotation.x) * Math.cos(rotation.y));
        return new Triple(dx, dy, dz);
    }

    public Pair<Float>projectTo2D(float x, float y, float z) {
        x -= cameraCoords.x;
        y -= cameraCoords.y;
        z -= cameraCoords.z;

        float cosY = (float)Math.cos(-cameraRotation.y);
        float sinY = (float)Math.sin(-cameraRotation.y);
        float xr = x * cosY + z * sinY;
        float zr2 = -x * sinY + z * cosY;
        x = xr;
        z = zr2;

        float cosX = (float)Math.cos(cameraRotation.x);
        float sinX = (float)Math.sin(cameraRotation.x);
        float yr = y * cosX - z * sinX;
        float zr = y * sinX + z * cosX;
        y = yr;
        z = zr;

        if(z < 0) return null;

        float d = FOV * x/z;
        float t = FOV * y/z;

//        if(Math.abs(y) > Math.abs(z) || Math.abs(x) > Math.abs(z)) return null; // only sohw visible nodes , wihtout it things in front of camera z is drawn off screen

        return new Pair<>(SCREEN_WIDTH * d/2f + SCREEN_WIDTH/2f, ((SCREEN_HEIGHT*t)/2f) + (SCREEN_HEIGHT/2f));
    }
    public Pair<Float>projectTo2DWithoutRotatingAgainstCamera(float x, float y, float z) {
        x -= cameraCoords.x;
        y -= cameraCoords.y;
        z -= cameraCoords.z;

        float d = x/z;
        float t = y/z;

//        if(Math.abs(y) > Math.abs(z) || Math.abs(x) > Math.abs(z)) return null; // only sohw visible nodes , wihtout it things in front of camera z is drawn off screen

        return new Pair<>(SCREEN_WIDTH * d/2f + SCREEN_WIDTH/2f, ((SCREEN_HEIGHT*t)/2f) + (SCREEN_HEIGHT/2f));
    }

    public static void main(String[] args) throws Exception {
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

    private Triple moveForward() {
        float dx = moveSpeed * (float) Math.sin(cameraRotation.y);
        float dz = moveSpeed * (float) Math.cos(cameraRotation.y);
        return new Triple(dx,0f, dz);
    }

    private Triple moveBackward() {
        float dx = -moveSpeed * (float) Math.sin(cameraRotation.y);
        float dz = -moveSpeed * (float) Math.cos(cameraRotation.y);
        return new Triple(dx,0f, dz);
    }

    private Triple moveRight() {
        float dx = moveSpeed * (float) Math.cos(cameraRotation.y);
        float dz = moveSpeed * (float) -Math.sin(cameraRotation.y);
        return new Triple(dx,0f, dz);
    }

    private Triple moveLeft() {
        float dx = -moveSpeed * (float)Math.cos(cameraRotation.y);
        float dz = moveSpeed * (float)Math.sin(cameraRotation.y);
        return new Triple(dx,0f, dz);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keysPressed[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keysPressed[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        float a = (float) Math.PI * -((2 * e.getY() / SCREEN_HEIGHT) - 1);// - because panel y starts from top
        if(a > 0)
            cameraRotation.x = Math.min(a,1.57f);
        else if(a < 0)
            cameraRotation.x = Math.max(a,-1.57f);

        cameraRotation.y = (float)Math.PI * ((2 * e.getX() / SCREEN_WIDTH) - 1);

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        buttonsPressed[e.getButton()] = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        buttonsPressed[e.getButton()] = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
