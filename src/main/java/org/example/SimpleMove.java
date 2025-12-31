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
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class SimpleMove extends Canvas implements Runnable, KeyListener, MouseMotionListener {

    public static final int SCREEN_WIDTH = 1080;
    public static final int SCREEN_HEIGHT = 720;

    Triple cameraCoords = new Triple(0f,0f,0f);
    Pair<Float> cameraRotation = new Pair<>(0f,0f);

    Cube[] cubes;

    List<Triple> floor = new ArrayList<>();


    public SimpleMove(Cube... cubes) {
        this.cubes = cubes;
        setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        addKeyListener(this);
        addMouseMotionListener(this);

        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                floor.add(new Triple(i*1f, j*1f, 0f));
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
            update();
            render(bs);
            try {
                Thread.sleep(16);   // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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

        g.setColor(Color.BLUE);

        for (Cube rec: cubes) {
            Pair<Float>[] projectedDots = getProjectedDotsForCube(rec);

            for(Pair<Integer> pair : Cube.edges){
                if(projectedDots[pair.x] == null || projectedDots[pair.y] == null) continue;
                g.draw(new Line2D.Float(
                        projectedDots[pair.x].x,
                        720 - projectedDots[pair.x].y,// - because panel y starts from top
                        projectedDots[pair.y].x,
                        720 - projectedDots[pair.y].y));// - because panel y starts from top
            }
        }

        g.dispose();
        bs.show();
    }

    private Pair<Float>[] getProjectedDotsForCube(Cube rec) {
        Pair<Float>[] projectedDots = (Pair<Float>[]) new Pair<?>[8];
        int count =-1;
        for(Triple point: rec.getPoints()) {
            count++;
            float[] coords = projectTo2D(point.x, point.y, point.z);
            if(coords == null) continue;
            projectedDots[count] = new Pair<>(coords[0], coords[1]);
        }
        return projectedDots;
    }

    private float[] projectTo2D(float x, float y, float z) {
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

        if(z <= 0) return null;

        float d = x/z;
        float t = y/z;

        if(Math.abs(y) > Math.abs(z) || Math.abs(x) > Math.abs(z)) return null;

        return new float[]{SCREEN_WIDTH * d/2f + SCREEN_WIDTH/2f, ((SCREEN_HEIGHT*t)/2f) + (SCREEN_HEIGHT/2f)};
    }

    public static void main(String[] args) {
        Frame frame = new Frame("Simple Moving Rectangle");
        Cube[] arr = new Cube[]{
                new Cube(0f,0f, 10f,1f),
                new Cube(2f,-2f, 10f,1f),
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
        if(e.getKeyCode() == 16) {
            cameraCoords.y -= 0.1f;
        }
        switch (e.getKeyChar()) {
            case 'w':
                moveForward();
                break;
            case 's' :
                moveBackward();
                break;
            case 'd' : cameraCoords.x += 0.1f; break;
            case 'a' : cameraCoords.x -= 0.1f; break;
            case ' ' : cameraCoords.y += 0.1f; break;
            case 'c' :
                System.out.println(cameraRotation); break;
        }
    }

    private void moveForward() {
        cameraCoords.x += 0.1f * (float) Math.sin(cameraRotation.y);
        cameraCoords.z += 0.1f * (float) Math.cos(cameraRotation.y);
    }

    private void moveBackward() {
        cameraCoords.x -= 0.1f * (float) Math.sin(cameraRotation.y);
        cameraCoords.z -= 0.1f * (float) Math.cos(cameraRotation.y);
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        cameraRotation.x = (float)Math.PI * -((2 * e.getY() / 720f) - 1);// - because panel y starts from top
        cameraRotation.y = (float)Math.PI * ((2 * e.getX() / 1080f) - 1);

    }
}
