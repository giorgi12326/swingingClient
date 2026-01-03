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
import java.util.*;

public class SimpleMove extends Canvas implements Runnable, KeyListener, MouseListener , MouseMotionListener {

    public static final float SCREEN_WIDTH = 1280f;
    public static final float SCREEN_HEIGHT = 720f;
    public static final float moveSpeed = 5f;
    public static  boolean deathCubeSpawnMode = false;
    public static boolean hit = false;
    public final Set<Integer> keysDown = new HashSet<>();
    public static float deltaTime = 1f;
    public static float FOV = 1;
    long lastTime = System.nanoTime();

    float timer = 0f; // class-level variable

    public static boolean swinging = false;
    public static boolean grapplingEquiped = false;

    Triple cameraCoords = new Triple(0f,0f,0f);
    public static Pair<Float> cameraRotation = new Pair<>(0f,0f);

    List<Cube> cubes;
    List<DeathCube> deathCubes = new ArrayList<>();
    List<Ray> rays = new ArrayList<>();
    List<Triple> floor = new ArrayList<>();

    public static final float GRAVITY = 10f; // units per second²
    public static float verticalSpeed = 0f;

    private boolean inAir;
    private Triple sum;

    Gun gun = new Gun(0,0,0);
    GrapplingHead grapplingHead = new GrapplingHead(0,0f,0);
    private Triple anchor;

    public SimpleMove(Cube... cubes) {
        this.cubes = new ArrayList<>(Arrays.stream(cubes).toList());
        setSize((int)SCREEN_WIDTH, (int)SCREEN_HEIGHT);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);

        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                floor.add(new Triple(i*1f, 0f, j*1f));
            }
        }
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        createBufferStrategy(2);   // double buffering
        BufferStrategy bs = getBufferStrategy();

        while (true) {
            long now = System.nanoTime();
            deltaTime = (now - lastTime) / 1_000_000_000f;
            lastTime = now;

            input();
            update();
            render(bs);

            try {
                Thread.sleep(5);   // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void input() {
        sum = new Triple(0f,0f,0f);

        if (keysDown.contains(KeyEvent.VK_W))
            sum = sum.add(moveForward());
        if (keysDown.contains(KeyEvent.VK_S))
            sum = sum.add(moveBackward());
        if (keysDown.contains(KeyEvent.VK_D))
            sum = sum.add(moveRight());
        if (keysDown.contains(KeyEvent.VK_A))
            sum = sum.add(moveLeft());
        if (keysDown.contains(KeyEvent.VK_G))
            deathCubeSpawnMode = true;
        if (keysDown.contains(KeyEvent.VK_H))
            deathCubeSpawnMode = false;

        if(keysDown.contains(KeyEvent.VK_SPACE) && !inAir) {
            verticalSpeed = 6f;
            inAir = true;
        }

        if(keysDown.contains(KeyEvent.VK_SHIFT))
            cameraCoords.y -= moveSpeed * deltaTime;
    }

    private void update() {
        for(Cube cube : cubes) {
            cube.update();
        }
        for(DeathCube deathCube : deathCubes) {
            deathCube.update();
        }

        verticalSpeed -= GRAVITY * deltaTime;
        float dy = verticalSpeed * deltaTime;
        sum.y += dy;
        cameraCoords.y += sum.y;

        for (Cube cube : cubes) {
            if (cube.isPointInCube(cameraCoords)) {
                if (sum.y > 0) {
                    cameraCoords.y = cube.y - cube.size / 2 - 0.0001f;
                    verticalSpeed = 0f;
                } else {
                    cameraCoords.y = cube.y + cube.size / 2 + 0.0001f;
                    verticalSpeed = 0f;
                    inAir = false;
                }
            }
        }
        if (cameraCoords.y <= 0f) {
            cameraCoords.y = 0.0001f;
            verticalSpeed = 0f;
            inAir = false;
        }

        cameraCoords.x += sum.x;
        for (Cube cube : cubes) {
            if (cube.isPointInCube(cameraCoords)) {
                if (sum.x > 0)
                    cameraCoords.x = cube.x - cube.size / 2 - 0.0001f;
                else
                    cameraCoords.x = cube.x + cube.size / 2 + 0.0001f;
            }
        }

        cameraCoords.z += sum.z;
        for (Cube cube : cubes) {
            if (cube.isPointInCube(cameraCoords)) {
                if (sum.z > 0)
                    cameraCoords.z = cube.z - cube.size / 2 - 0.0001f;
                else
                    cameraCoords.z = cube.z + cube.size / 2 + 0.0001f;
            }
        }
        if(swinging) {
            swingAround(anchor);
        }

        if(grapplingHead.shot && grapplingHead.flying){
            grapplingHead.x += grapplingHead.direction.x * 10f * deltaTime;
            grapplingHead.y += grapplingHead.direction.y * 10f * deltaTime;
            grapplingHead.z += grapplingHead.direction.z * 10f * deltaTime;
        }

        if(!grapplingHead.shot){
            grapplingHead.x = cameraCoords.x + 0.1f;
            grapplingHead.y = cameraCoords.y;
            grapplingHead.z = cameraCoords.z + 1f;
        }

        gun.x = cameraCoords.x + 0.1f;
        gun.y = cameraCoords.y;
        gun.z = cameraCoords.z + 0.3f;

        if(grapplingHead.shot)
            for(Cube cube : cubes) {
                if(cube.isPointInCube(grapplingHead.getNodes()[16])) {
                    swinging = true;
                    grapplingHead.flying = false;
                    anchor = new Triple(cube.x + cube.size / 2f, cube.y + cube.size / 2f, cube.z + cube.size / 2f);
                }
            }

        for (Ray ray : rays) {
            for (int j = deathCubes.size()-1; j >= 0; j--) {
                DeathCube deathCube = deathCubes.get(j);
                if (ray.rayIntersectsCube(
                        deathCube.x - deathCube.size/2f, deathCube.y - deathCube.size/2f, deathCube.z - deathCube.size/2f,
                        deathCube.x + deathCube.size/2f, deathCube.y + deathCube.size/2f, deathCube.z + deathCube.size/2f))
                    deathCubes.remove(j);
            }
        }
        boolean localHit = false;
        for(DeathCube deathCube : deathCubes) {
            if(deathCube.isPointInCube(cameraCoords))
                localHit = true;
        }

        timer += deltaTime;

        if (deathCubeSpawnMode && timer >= 3f) {
            timer = 0f;
            spawnCubeRandomlyAtDistance(64f);
        }

        for (int i = deathCubes.size() - 1; i >= 0; i--) {
            DeathCube deathCube = deathCubes.get(i);
            if(deathCube.markedAsDeleted || deathCube.y < 0)
                deathCubes.remove(i);
        }

        hit = localHit;

        rays.clear();
    }

    private void spawnCubeRandomlyAtDistance(float radius) {
        float x = (float)(Math.random() * 2 * radius - radius);
        float z = (float)(Math.random() * 2 * radius - radius);
        float y = 10f;
        deathCubes.add(new DeathCube(cameraCoords.x + x, cameraCoords.y + y, cameraCoords.z + z, cameraCoords, 1f));

    }

    private void render(BufferStrategy bs) {
        Graphics gj = bs.getDrawGraphics();
        Graphics2D g = (Graphics2D) gj;

        clearScreen(g);

        drawCrosshair(g);

        g.setColor(Color.BLUE);

        for (Ray ray : rays)
            ray.draw(g, this);

        for(Triple point : floor)
            point.draw(g,this);

        for (Cube cube: cubes)
            cube.draw(g, this);

        for (DeathCube cube: deathCubes)
            cube.draw(g, this);

        g.setColor(Color.YELLOW);
        gun.draw(g, this);

        if(grapplingEquiped) {
            grapplingHead.drawEdges(g, this);

            Pair<Float>[] projectedDotsForGun = gun.getProjectedDotsForGun(this);

            Pair<Float>[] hookProjected = grapplingHead.getProjectedDotsForGun(this);
            if(projectedDotsForGun[Gun.edges.get(12).x] != null && hookProjected[GrapplingHead.edges.get(16).y] != null)
                g.draw(new Line2D.Float(
                        projectedDotsForGun[Gun.edges.get(12).x].x,
                        SCREEN_HEIGHT -  projectedDotsForGun[Gun.edges.get(12).x].y,// - because panel y starts from top
                        hookProjected[GrapplingHead.edges.get(16).y].x,
                        SCREEN_HEIGHT - hookProjected[GrapplingHead.edges.get(16).y].y));// - because panel y starts from top


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

    private static void drawCrosshair(Graphics2D g) {
        g.setColor(Color.lightGray);
        g.fill(new Rectangle2D.Float(SCREEN_WIDTH/2f - 10f, SCREEN_HEIGHT/2f-2f, 20f, 4f));
        g.fill(new Rectangle2D.Float(SCREEN_WIDTH/2f - 2f, SCREEN_HEIGHT/2f-10f, 4f, 20f));
    }

    private Triple normalization(Pair<Float> rotation) {
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

    public static void main(String[] args) {
        Frame frame = new Frame("Simple Moving Rectangle");
        Cube[] arr = new Cube[]{
                new Cube(0.5f,0.5f, 3.5f,1f),
                new Cube(0.5f,1.5f, 5.5f,1f),
                new Cube(0.5f,2.5f, 6.5f,1f),
                new Cube(0.5f,3.5f, 9.5f,1f),
                new Cube(0.5f,3.5f, 12.5f,1f),
                new Cube(0.5f,3.5f, 15.5f,1f),
                new Cube(0.5f,4.5f, 18.5f,1f),
                new Cube(0.5f,7.5f, 23.5f,1f),
                new Cube(0.5f,4.5f, 30.5f,1f),
        };

        SimpleMove canvas = new SimpleMove(arr);
        frame.add(canvas);
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });

        canvas.start();
    }


    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_9)
            FOV*=1.1f;
        if(e.getKeyCode() == KeyEvent.VK_0)
            FOV*=0.9f;
        if(e.getKeyCode() == KeyEvent.VK_Y)
            swinging = true;
        if(e.getKeyCode() == KeyEvent.VK_T)
            swinging = false;
        keysDown.add(e.getKeyCode());
    }

    private Triple moveForward() {
        float dx = moveSpeed * (float) Math.sin(cameraRotation.y) * deltaTime;
        float dz = moveSpeed * (float) Math.cos(cameraRotation.y) * deltaTime;
        return new Triple(dx,0f, dz);
    }

    private Triple moveBackward() {
        float dx = -moveSpeed * deltaTime * (float) Math.sin(cameraRotation.y);
        float dz = -moveSpeed * deltaTime * (float) Math.cos(cameraRotation.y);
        return new Triple(dx,0f, dz);
    }

    private Triple moveRight() {
        float dx = moveSpeed * deltaTime * (float) Math.cos(cameraRotation.y);
        float dz = moveSpeed * deltaTime * (float) -Math.sin(cameraRotation.y);
        return new Triple(dx,0f, dz);
    }

    private Triple moveLeft() {
        float dx = -moveSpeed * deltaTime * (float)Math.cos(cameraRotation.y);
        float dz = moveSpeed * deltaTime * (float)Math.sin(cameraRotation.y);
        return new Triple(dx,0f, dz);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keysDown.remove(e.getKeyCode());
    }

    public void swingAround(Triple anchor) {
        Triple toAnchor = anchor.sub(cameraCoords).normalize();
        Triple tangent = new Triple(0f, -toAnchor.z, toAnchor.y).normalize();
        cameraCoords = cameraCoords.add(tangent.scale(moveSpeed * deltaTime * 2));
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
    public void mousePressed(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1) {
            Ray ray = new Ray(new Triple(cameraCoords), new Pair<>(cameraRotation), 5f);
            if(grapplingEquiped && !grapplingHead.shot){
                ray.deltaDirection = normalization(ray.direction);

                prepareHookForFlying(ray);
            }
            else {
                rays.add(ray);
                ray.deltaDirection = normalization(ray.direction);
            }
        }
        if(e.getButton() == MouseEvent.BUTTON2) {
            Triple normalization = normalization(cameraRotation);
            floor.add (new Triple(cameraCoords.x + normalization.x,cameraCoords.y + normalization.y, cameraCoords.z + normalization.z));
        }
        if(e.getButton() == MouseEvent.BUTTON3) {
            grapplingEquiped = !grapplingEquiped;
            swinging = false;
            grapplingHead.shot = false;
        }

    }

    private void prepareHookForFlying(Ray ray) {
        grapplingHead.direction = normalization(ray.direction);
        grapplingHead.rotation = new Pair<>(cameraRotation.x, cameraRotation.y);
        Triple newPosition = new Triple(grapplingHead.x, grapplingHead.y, grapplingHead.z).rotateXY(cameraCoords, grapplingHead.rotation);
        grapplingHead.x = newPosition.x;
        grapplingHead.y = newPosition.y;
        grapplingHead.z = newPosition.z;
        grapplingHead.shot = true;
        grapplingHead.flying = true;

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }
}
