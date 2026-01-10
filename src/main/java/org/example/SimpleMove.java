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
    public static final float moveSpeed = 10f;
    public static  boolean deathCubeSpawnMode = false;
    public static boolean hit = false;
    public final Set<Integer> keysDown = new HashSet<>();
    public static float deltaTime = 1f;
    public static float FOV = 1;
    long lastTime = System.nanoTime();

    long bulletShotLastTime = System.currentTimeMillis();
    long deathCubeLastSpawnTime = System.currentTimeMillis(); // class-level variable

    public static boolean swinging = false;
    public static boolean grapplingEquipped = false;

    public static Triple cameraCoords = new Triple(0f,0f,0f);
    public static Pair<Float> cameraRotation = new Pair<>(0f,0f);

    List<Cube> cubes;
    List<DeathCube> deathCubes = new ArrayList<>();
    List<Triple> floor = new ArrayList<>();
    List<BulletHead> bullets = new ArrayList<>();
    BulletHead heldBullet = new BulletHead();

    public static final float GRAVITY = 10f;
    public static float speedX = 0f;
    public static float speedY = 0f;
    public static float speedZ = 0f;

    private boolean inAir;
    private Triple sum;

    Gun gun = new Gun(0,0,0);
    GrapplingHead grapplingHead = new GrapplingHead(0,0f,0);
    private Triple anchor;

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
            speedY = 6f;
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
        if(!swinging)
            speedY -= GRAVITY * deltaTime;
        float dy = speedY * deltaTime;
        sum.y += dy;
        cameraCoords.y += sum.y;

        for (Cube cube : cubes) {
            if (cube.isPointInCube(cameraCoords)) {
                if (sum.y > 0) {
                    cameraCoords.y = cube.y - cube.size / 2 - 0.0001f;
                } else {
                    cameraCoords.y = cube.y + cube.size / 2 + 0.0001f;
                    speedY = 0f;
                    inAir = false;
                }
                speedY = 0f;

            }
        }
        if (cameraCoords.y <= 0f) {
            cameraCoords.y = 0.0001f;
            speedY = 0f;
            inAir = false;
        }

        moveCharacter();

        for (Cube cube : cubes) {
            if (cube.isPointInCube(cameraCoords)) {
                if (speedZ > 0)
                    cameraCoords.z = cube.z - cube.size / 2 - 0.0001f;
                else if (speedZ < 0)
                    cameraCoords.z = cube.z + cube.size / 2 + 0.0001f;

                speedZ = 0f;
            }
        }

        if(swinging) {
            swingAround(anchor);
        }

        grapplingHead.update();

        if(!grapplingHead.shot){
            grapplingHead.x = cameraCoords.x + 0.1f;
            grapplingHead.y = cameraCoords.y;
            grapplingHead.z = cameraCoords.z + 1f;
        }

        for(BulletHead bulletHead: bullets) {
            bulletHead.update();
        }

        if(heldBullet != null){
            heldBullet.x = cameraCoords.x;
            heldBullet.y = cameraCoords.y- 0.15f;
            heldBullet.z = cameraCoords.z + 0.8f;

        }
        else if(System.currentTimeMillis() - bulletShotLastTime > 200){
            heldBullet = new BulletHead();
            heldBullet.x = 1000f;
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

        for (BulletHead bullet : bullets) {
            for (int j = deathCubes.size()-1; j >= 0; j--) {
                DeathCube deathCube = deathCubes.get(j);
                if (deathCube.isPointInCube(bullet.getNodes()[8]))
                    deathCubes.remove(j);
            }
        }
        boolean localHit = false;
        for(DeathCube deathCube : deathCubes) {
            if(deathCube.isPointInCube(cameraCoords))
                localHit = true;
        }

        if (deathCubeSpawnMode && System.currentTimeMillis() - deathCubeLastSpawnTime > 1000) {
            deathCubeLastSpawnTime = System.currentTimeMillis();
            spawnCubeRandomlyAtDistance(64f);
        }

        for (int i = deathCubes.size() - 1; i >= 0; i--) {
            DeathCube deathCube = deathCubes.get(i);
            if(deathCube.markedAsDeleted || deathCube.y < 0)
                deathCubes.remove(i);
        }

        for (int i = bullets.size() - 1; i >= 0; i--) {
            BulletHead bullet = bullets.get(i);
            if(bullet.markAsDeleted)
                bullets.remove(i);
        }

        hit = localHit;

    }

    private void moveCharacter() {
        float inputX = sum.x;
        float inputZ = sum.z;

        float inputLength = (float)Math.sqrt(inputX*inputX + inputZ*inputZ);
        if(inputLength > 0.001f) {
            inputX /= inputLength;
            inputZ /= inputLength;
        }

        inputX *= moveSpeed;
        inputZ *= moveSpeed;

        final float DRAG_MOVE = 0.1f;
        final float DRAG_IDLE = 12.0f;
        boolean notMoving = sum.x == 0 && sum.z == 0;

        float drag = DRAG_MOVE;

        float dot = speedX * inputX + speedZ * inputZ;

        if (notMoving || dot < 0f) {
            drag = DRAG_IDLE;
        }

        speedX -= speedX * drag * deltaTime;
        speedZ -= speedZ * drag * deltaTime;

        speedX += inputX * deltaTime;
        speedZ += inputZ * deltaTime;

        float maxSpeed = 5f;
        float combinedSpeed = (float)Math.sqrt(speedX*speedX + speedZ*speedZ);
        if(combinedSpeed > maxSpeed) {
            speedX = speedX / combinedSpeed * maxSpeed;
            speedZ = speedZ / combinedSpeed * maxSpeed;
        }

        cameraCoords.x += speedX * deltaTime;
        cameraCoords.z += speedZ * deltaTime;
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
        if(heldBullet != null && (!grapplingEquipped || grapplingHead.shot))
            heldBullet.drawEdges(g, this);

        for (BulletHead bullet : bullets){
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

    public static void main(String[] args) {
        Frame frame = new Frame("Simple Moving Rectangle");
        SimpleMove canvas = new SimpleMove();
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
    public void keyReleased(KeyEvent e) {
        keysDown.remove(e.getKeyCode());
    }

    public void swingAround(Triple anchor) {
        Triple toAnchor = anchor.sub(cameraCoords).normalize();
        Triple tangent = toAnchor.normalize();
        cameraCoords = cameraCoords.add(tangent.scale(moveSpeed*2 * deltaTime));
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
            if(grapplingEquipped && !grapplingHead.shot){
                prepareShootableForFlying(ray.direction, grapplingHead);
            }
            else {
                prepareBulletForFlying(ray.direction,heldBullet);
            }
        }
        if(e.getButton() == MouseEvent.BUTTON2) {
            Triple normalization = rotationToDirection(cameraRotation);
            floor.add (new Triple(cameraCoords.x + normalization.x,cameraCoords.y + normalization.y, cameraCoords.z + normalization.z));
        }
        if(e.getButton() == MouseEvent.BUTTON3) {
            grapplingEquipped = !grapplingEquipped;
            swinging = false;
            grapplingHead.shot = false;
        }

    }

    private void prepareBulletForFlying(Pair<Float> direction, BulletHead bulletHead) {
        if(bulletHead == null)
            return;

        prepareShootableForFlying(direction, bulletHead);

        bullets.add(heldBullet);
        heldBullet = null;
        bulletShotLastTime = System.currentTimeMillis();

    }

    private void prepareShootableForFlying(Pair<Float> direction, Shootable shootable) {
        shootable.direction = rotationToDirection(direction);
        shootable.rotation = new Pair<>(cameraRotation.x, cameraRotation.y);
        Triple newPosition = new Triple(shootable.x, shootable.y, shootable.z).rotateXY(cameraCoords, shootable.rotation);
        shootable.x = newPosition.x;
        shootable.y = newPosition.y;
        shootable.z = newPosition.z;
        shootable.shot = true;
        shootable.flying = true;
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
