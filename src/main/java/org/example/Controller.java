package org.example;

import java.awt.event.*;

import static org.example.SimpleMove.*;

public class Controller implements MouseListener, MouseMotionListener, KeyListener {

    public Controller(SimpleMove simpleMove) {
        simpleMove.addKeyListener(this);
        simpleMove.addMouseMotionListener(this);
        simpleMove.addMouseListener(this);
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyChar() == 'o') {
            INTERP_DELAY_MS = Math.max(0, INTERP_DELAY_MS - 1);
            return;
        }
        if(e.getKeyChar() == 'p') {
            INTERP_DELAY_MS++;
            return;
        }
        if(e.getKeyChar() == 'k') {
            FOV -= 0.1f;
            return;
        }
        if(e.getKeyChar() == 'l') {
            FOV += 0.1f;
            return;
        }
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
