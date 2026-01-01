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

@SuppressWarnings("unchecked")
public class SimpleMove extends Canvas implements Runnable, KeyListener, MouseListener , MouseMotionListener {

    public static final float SCREEN_WIDTH = 1080f;
    public static final float SCREEN_HEIGHT = 720f;
    public final Set<Integer> keysDown = new HashSet<>();

    Triple cameraCoords = new Triple(0f,0f,0f);
    Pair<Float> cameraRotation = new Pair<>(0f,0f);

    List<Cube> cubes;
    List<Ray> rays = new ArrayList<>();
    List<Triple> floor = new ArrayList<>();

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
            input();
            update();
            render(bs);
            try {
                Thread.sleep(16);   // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void input() {
        if(keysDown.contains(KeyEvent.VK_W))
            moveForward();
        if(keysDown.contains(KeyEvent.VK_S))
            moveBackward();
        if(keysDown.contains(KeyEvent.VK_D))
            moveRight();
        if(keysDown.contains(KeyEvent.VK_A))
            moveLeft();
        if(keysDown.contains(KeyEvent.VK_SPACE))
            cameraCoords.y += 0.1f;
        if(keysDown.contains(KeyEvent.VK_SHIFT))
            cameraCoords.y -= 0.1f;
    }

    private void update() {
        for(Cube cube : cubes) {
            cube.update();
        }
    }

    private void render(BufferStrategy bs) {
        Graphics gj = bs.getDrawGraphics();
        Graphics2D g = (Graphics2D) gj;

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.lightGray);
        g.fill(new Rectangle2D.Float(SCREEN_WIDTH/2f - 10f, SCREEN_HEIGHT/2f-2f, 20f, 4f));
        g.fill(new Rectangle2D.Float(SCREEN_WIDTH/2f - 2f, SCREEN_HEIGHT/2f-10f, 4f, 20f));

        g.setColor(Color.BLUE);
        for (int i = rays.size()-1; i >= 0 ; i--) {
            Ray ray = rays.get(i);
            Pair<Float> projected = projectTo2D(ray.position.x, ray.position.y, ray.position.z);
            Pair<Float> nextProjected = projectTo2D(ray.position.x + ray.deltaDirection.x, ray.position.y + ray.deltaDirection.y, ray.position.z + ray.deltaDirection.z);
            if(projected == null || nextProjected == null) continue;
            for (int j = cubes.size()-1; j >= 0; j--) {
                Cube cube = cubes.get(j);
                if (rayIntersectsCube(
                        ray.position.x, ray.position.y, ray.position.z,
                        ray.deltaDirection.x, ray.deltaDirection.y, ray.deltaDirection.z,
                        cube.x - cube.size/2f, cube.y - cube.size/2f, cube.z - cube.size/2f,
                        cube.x + cube.size/2f, cube.y + cube.size/2f, cube.z + cube.size/2f
                ))
                    cubes.remove(j);

            }
            g.draw(new Line2D.Float(
                    projected.x,
                    SCREEN_HEIGHT - projected.y,
                    nextProjected.x,
                    SCREEN_HEIGHT - nextProjected.y
            ));

        }
        rays.clear();
        for(Triple point : floor) {
            Pair<Float> projected = projectTo2D(point.x, point.y, point.z);
            if(projected == null) continue;
            g.fill(new Rectangle2D.Float(projected.x, SCREEN_HEIGHT - projected.y, 5f, 5f));
        }

        for (Cube rec: cubes) {
            Pair<Float>[] projectedDots = getProjectedDotsForCube(rec);

            for(Pair<Integer> pair : Cube.edges){
                if(projectedDots[pair.x] == null || projectedDots[pair.y] == null) continue;
                g.draw(new Line2D.Float(
                        projectedDots[pair.x].x,
                        SCREEN_HEIGHT - projectedDots[pair.x].y,// - because panel y starts from top
                        projectedDots[pair.y].x,
                        SCREEN_HEIGHT - projectedDots[pair.y].y));// - because panel y starts from top
            }
        }

        g.dispose();
        bs.show();
    }

    private Triple normalization(Ray ray) {
        float dx = (float)(Math.cos(ray.direction.x) * Math.sin(ray.direction.y));
        float dy = (float)(Math.sin(ray.direction.x));
        float dz = (float)(Math.cos(ray.direction.x) * Math.cos(ray.direction.y));
        return new Triple(dx, dy, dz);
    }

    boolean rayIntersectsCube(
            float ox, float oy, float oz,
            float dx, float dy, float dz,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ
    ) {
        float tMin = (minX - ox) / dx;
        float tMax = (maxX - ox) / dx;
        if (tMin > tMax) { float tmp = tMin; tMin = tMax; tMax = tmp; }

        float tyMin = (minY - oy) / dy;
        float tyMax = (maxY - oy) / dy;
        if (tyMin > tyMax) { float tmp = tyMin; tyMin = tyMax; tyMax = tmp; }

        if (tMin > tyMax || tyMin > tMax) return false;

        if (tyMin > tMin) tMin = tyMin;
        if (tyMax < tMax) tMax = tyMax;

        float tzMin = (minZ - oz) / dz;
        float tzMax = (maxZ - oz) / dz;
        if (tzMin > tzMax) { float tmp = tzMin; tzMin = tzMax; tzMax = tmp; }

        if (tMin > tzMax || tzMin > tMax) return false;

        return tMax >= 0;
    }


    private Pair<Float>[] getProjectedDotsForCube(Cube rec) {
        Pair<Float>[] projectedDots = (Pair<Float>[]) new Pair<?>[8];
        int count =-1;
        for(Triple point: rec.getPoints()) {
            count++;
            Pair<Float> projected = projectTo2D(point.x, point.y, point.z);
            if(projected == null) continue;
            projectedDots[count] = projected;
        }
        return projectedDots;
    }

    private Pair<Float>projectTo2D(float x, float y, float z) {
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

        float d = x/z;
        float t = y/z;

//        if(Math.abs(y) > Math.abs(z) || Math.abs(x) > Math.abs(z)) return null; // only sohw visible nodes , wihtout it things in front of camera z is drawn off screen

        return new Pair<>(SCREEN_WIDTH * d/2f + SCREEN_WIDTH/2f, ((SCREEN_HEIGHT*t)/2f) + (SCREEN_HEIGHT/2f));
    }

    public static void main(String[] args) {
        Frame frame = new Frame("Simple Moving Rectangle");
        Cube[] arr = new Cube[]{
                new Cube(0.5f,0.5f, 3.5f,1f),
                new Cube(0.5f,2.5f, 3.5f,1f),
                new Cube(0.5f,4.5f, 3.5f,1f),
                new Cube(0.5f,6.5f, 3.5f,1f),
                new Cube(2.5f,0.5f, 3.5f,1f),
                new Cube(2.5f,2.5f, 3.5f,1f),
                new Cube(2.5f,4.5f, 3.5f,1f),
                new Cube(2.5f,6.5f, 3.5f,1f),
                new Cube(4.5f,0.5f, 3.5f,1f),
                new Cube(4.5f,2.5f, 3.5f,1f),
                new Cube(4.5f,4.5f, 3.5f,1f),
                new Cube(4.5f,6.5f, 3.5f,1f),
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
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        keysDown.add(e.getKeyCode());
    }

    private void moveForward() {
        cameraCoords.x += 0.1f * (float) Math.sin(cameraRotation.y);
        cameraCoords.z += 0.1f * (float) Math.cos(cameraRotation.y);
    }

    private void moveBackward() {
        cameraCoords.x -= 0.1f * (float) Math.sin(cameraRotation.y);
        cameraCoords.z -= 0.1f * (float) Math.cos(cameraRotation.y);
    }

    private void moveRight() {
        cameraCoords.x += 0.1f * (float) Math.cos(cameraRotation.y);
        cameraCoords.z += 0.1f * (float) -Math.sin(cameraRotation.y);
    }
    private void moveLeft() {
        cameraCoords.x -= 0.1f * (float)Math.cos(cameraRotation.y);
        cameraCoords.z += 0.1f * (float)Math.sin(cameraRotation.y);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keysDown.remove(e.getKeyCode());
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
        Ray ray = new Ray(new Triple(cameraCoords), new Pair<>(cameraRotation), 5f);
        rays.add(ray);
        ray.deltaDirection = normalization(ray);
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
}
