package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
public class FireEffect extends EnemyBullet {
    private long startTime;
    private boolean dead;

    public FireEffect(double x, double y) {
        super(x,y);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void update() {
        // Remove the fire effect after 1 second
        new Thread(() -> {
            if (System.currentTimeMillis() - startTime > 4) {
                this.dead = true;
            }
        }).start();
    }
    @Override
    public void render(GraphicsContext gc) {
        // Draw the fire effect image
        Image fireImage = new Image(getClass().getResourceAsStream("/exp.png"));
        gc.drawImage(fireImage, x, y, 30, 30); // Adjust size as needed
    }
    @Override
    public boolean isDead() {
        return this.dead;
    }

}
