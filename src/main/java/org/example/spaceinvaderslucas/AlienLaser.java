package org.example.spaceinvaderslucas;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class AlienLaser {
    private final double x;
    private double y;
    private final double speed;
    private final double width = 4;
    private final double height = 15;

    public AlienLaser(double x, double y, double speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
    }

    public void update(double deltaTime) {
        y += speed * deltaTime;
    }

    public void render(GraphicsContext gc) {
        // Save original color
        Color originalColor = (Color) gc.getFill();

        // Draw the laser as a simple red rectangle
        gc.setFill(Color.GREEN);
        gc.fillRect(x, y, width, height);

        // Restore original color
        gc.setFill(originalColor);
    }

    public boolean isOffScreen(double screenHeight) {
        return y > screenHeight;
    }

    public boolean checkCollision(GameObject player) {
        if (player == null) return false;

        double playerX = player.getX();
        double playerY = player.getY();

        int playerWidth = 40;
        int playerHeight = 30;

        return x < playerX + playerWidth &&
               x + width > playerX &&
               y < playerY + playerHeight &&
               y + height > playerY;
    }
}
