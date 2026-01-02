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
    public final Set<Integer> keysDown = new HashSet<>();
    public static float deltaTime = 1f;
    public static float FOV = 1;
    long lastTime = System.nanoTime();

    Triple cameraCoords = new Triple(0f,0f,0f);
    Pair<Float> cameraRotation = new Pair<>(0f,0f);

    List<Cube> cubes;
    List<Ray> rays = new ArrayList<>();
    List<Triple> floor = new ArrayList<>();

    public static final float GRAVITY = 10f; // units per second²
    public static float verticalSpeed = 0f;

    private boolean inAir;
    private Triple sum;

    Gun gun = new Gun(0,0,0);

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
            sum.add(moveForward());
        if (keysDown.contains(KeyEvent.VK_S))
            sum.add(moveBackward());
        if (keysDown.contains(KeyEvent.VK_D))
            sum.add(moveRight());
        if (keysDown.contains(KeyEvent.VK_A))
            sum.add(moveLeft());

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


        verticalSpeed -= GRAVITY * deltaTime;
        float dy = verticalSpeed * deltaTime;
        sum.y += dy;
        cameraCoords.y += sum.y;

        for(Cube cube: cubes) {
            if(cube.isInCube(cameraCoords)) {
                if(sum.y > 0) {
                    cameraCoords.y = cube.y - cube.size/2 - 0.0001f;
                    verticalSpeed = 0f;
                }
                else {
                    cameraCoords.y = cube.y + cube.size/2 + 0.0001f;
                    verticalSpeed = 0f;
                    inAir = false;
                }
            }
        }
        if(cameraCoords.y <= 0f) {
            cameraCoords.y = 0.0001f;
            verticalSpeed = 0f;
            inAir = false;
        }

        cameraCoords.x += sum.x;
        for(Cube cube : cubes) {
            if (cube.isInCube(cameraCoords)) {
                if (sum.x > 0)
                    cameraCoords.x = cube.x - cube.size / 2 - 0.0001f;
                else
                    cameraCoords.x = cube.x + cube.size / 2 + 0.0001f;
            }
        }

        cameraCoords.z += sum.z;
        for(Cube cube : cubes) {
            if (cube.isInCube(cameraCoords)) {
                if (sum.z > 0)
                    cameraCoords.z = cube.z - cube.size / 2 - 0.0001f;
                else
                    cameraCoords.z = cube.z + cube.size / 2 + 0.0001f;
            }
        }

        gun.x = cameraCoords.x  + 0.1f;
        gun.y = cameraCoords.y  ;
        gun.z = cameraCoords.z  + 0.3f;

        for (Ray ray : rays) {
            for (int j = cubes.size()-1; j >= 0; j--) {
                Cube cube = cubes.get(j);
                if (ray.rayIntersectsCube(
                        cube.x - cube.size/2f, cube.y - cube.size/2f, cube.z - cube.size/2f,
                        cube.x + cube.size/2f, cube.y + cube.size/2f, cube.z + cube.size/2f))
                    cubes.remove(j);
            }
        }
//        rays.clear();
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

        g.setColor(Color.YELLOW);
        gun.draw(g, this);

        g.drawString("FPS: " + (int)(1/deltaTime), 30, 30);

        g.dispose();
        bs.show();
    }

    private void clearScreen(Graphics2D g) {
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
                new Cube(0.5f,4.5f, 23.5f,1f),
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
            rays.add(ray);
            ray.deltaDirection = normalization(ray.direction);
        }
        if(e.getButton() == MouseEvent.BUTTON2) {
            Triple normalization = normalization(cameraRotation);
            floor.add (new Triple(cameraCoords.x + normalization.x,cameraCoords.y + normalization.y, cameraCoords.z + normalization.z));
        }

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
